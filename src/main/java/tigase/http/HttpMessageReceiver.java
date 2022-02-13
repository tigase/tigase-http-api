/*
 * Tigase HTTP API component - Tigase HTTP API component
 * Copyright (C) 2013 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.http;

import tigase.component.exceptions.ComponentException;
import tigase.http.api.HttpServerIfc;
import tigase.http.modules.Module;
import tigase.http.stats.HttpStatsCollector;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "http", parent = Kernel.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SetupMode})
public class HttpMessageReceiver
		extends AbstractMessageReceiver
		implements PacketWriter, RegistrarBean {

	private static final Logger log = Logger.getLogger(HttpMessageReceiver.class.getCanonicalName());
	private static final EnumSet<StanzaType> resultTypes = EnumSet.of(StanzaType.error, StanzaType.result);

	@Inject
	private List<Module> activeModules = new ArrayList<>();
	@Inject
	private HttpServerIfc httpServer;
	private Map<String, Module> modules = new ConcurrentHashMap<String, Module>();
	private ConcurrentHashMap<String, CompletableFuture<Packet>> pendingRequest = new ConcurrentHashMap<>();
	private ScheduledExecutorService scheduler;
	private boolean delayStartup = false;

	@Inject(nullAllowed = true)
	private List<HttpStatsCollector> statsCollectors = Collections.emptyList();

	@Override
	public void everyHour() {
		for (Module m : modules.values()) {
			m.everyHour();
		}
	}

	@Override
	public void everyMinute() {
		for (Module m : modules.values()) {
			m.everyMinute();
		}
	}

	@Override
	public void everySecond() {
		for (Module m : modules.values()) {
			m.everySecond();
		}
	}

	@Override
	public void start() {
		scheduler = Executors.newScheduledThreadPool(2);
		super.start();
	}

	@Override
	public void stop() {
		scheduler.shutdown();
		super.stop();
	}

	@Override
	public String getDiscoDescription() {
		return "HTTP server integration module";
	}

	@Override
	public boolean isSubdomain() {
		return true;
	}

	public void setActiveModules(List<Module> activeModules) {
		List<Module> toUnregister = new ArrayList<>(this.activeModules);
		toUnregister.removeAll(activeModules);
		List<Module> toRegister = new ArrayList<>(activeModules);
		toRegister.removeAll(this.activeModules);

		for (Module module : toUnregister) {
			modules.remove(module.getName());
		}

		JID componentJid = JID.jidInstanceNS(getName() + "." + this.getDefHostName().getDomain());
		for (Module module : toRegister) {
			module.init(componentJid, getName(), HttpMessageReceiver.this);
			modules.put(module.getName(), module);
		}

		this.activeModules = activeModules;
	}

	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {
		if (jid.getLocalpart() == null) {
			return super.getDiscoInfo(node, jid, from);
		} else {
			Module module = modules.get(jid.getLocalpart());
			if (module != null) {
				return module.getDiscoInfo(node, isAdmin(from));
			} else {
				return null;
			}
		}
	}

	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {
		if (jid.getDomain().startsWith(getName() + ".")) {
			if (jid.getLocalpart() != null) {
				Module module = modules.get(jid.getLocalpart());
				if (module != null) {
					return module.getDiscoItems(node, jid, from);
				} else {
					return Collections.emptyList();
				}
			} else {
				if (node == null) {
					List<Element> items = new ArrayList<Element>();
					for (Module module : modules.values()) {
						items.addAll(module.getDiscoItems(node, jid, from));
					}
					List<Element> pitems = super.getDiscoItems(node, jid, from);
					if (pitems != null) {
						items.addAll(pitems);
					}
					return items;
				} else {
					return super.getDiscoItems(node, jid, from);
				}
			}
			//return getServiceEntity().getDiscoItems(node, jid.toString());
		} else {
			return super.getDiscoItems(node, jid, from);
		}
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);

		modules.values().forEach(m -> m.getStatistics(getName(), list));
		statsCollectors.forEach(col -> col.getStatistics(getName(), list));
	}

	@Override
	public void processPacket(Packet packet) {
		boolean handled = false;

		if (resultTypes.contains(packet.getType())) {
			handled = processResultPacket(packet);
		}

		if (!handled && packet.getStanzaTo() != null && packet.getStanzaTo().getLocalpart() != null) {
			Module module = modules.get(packet.getStanzaTo().getLocalpart());
			if (module != null) {
				handled = module.processPacket(packet);
			}
		}

		// send error result if packet was not handled
		if (!handled) {
			// do not send errors if we received <iq/> with type of "result" or "error" as timeout could be reached
			// and there is no point in responding with errors for those requests
			if (packet.getElemName() != Iq.ELEM_NAME || !resultTypes.contains(packet.getType())) {
				try {
					// we can only process response we are waiting for so return error if packet is not expected
					addOutPacket(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, false));
				} catch (PacketErrorTypeException ex) {
					log.log(Level.FINEST, "packet processing type error", ex);
				}
			}
		}
	}
	
	@Override
	public boolean write(Module module, Packet packet) {
		JID from = module.getJid().copyWithoutResource();
		if (packet.getStanzaFrom() == null) {
			packet.initVars(from, packet.getStanzaTo());
		}
		String id = packet.getAttributeStaticStr("id");
		if (id == null) {
			id = UUID.randomUUID().toString();
			packet.getElement().setAttribute("id", id);
			packet.initVars(packet.getStanzaFrom(), packet.getStanzaTo());
		}
		packet.setPacketFrom(from);

		return addOutPacket(packet);
	}

	@Override
	public CompletableFuture<Packet> write(Module module, Packet packet, Integer timeout) {
		String uuid = UUID.randomUUID().toString();
		// changed to comply with clustering routing
		JID from = getComponentId().copyWithResourceNS(uuid);//module.getJid().copyWithResourceNS(uuid);
		if (packet.getStanzaFrom() == null) {
			packet.initVars(from, packet.getStanzaTo());
		}
		String id = packet.getAttributeStaticStr("id");
		if (id == null) {
			id = UUID.randomUUID().toString();
			packet.getElement().setAttribute("id", id);
			packet.initVars(packet.getStanzaFrom(), packet.getStanzaTo());
		}
		packet.setPacketFrom(from);

		if (packet.getStanzaFrom() != null && !this.isLocalDomainOrComponent(packet.getStanzaFrom().toString())) {
			if (this.isAdmin(packet.getStanzaFrom())) {
				packet.setPermissions(Permissions.ADMIN);
			} else {
				packet.setPermissions(Permissions.AUTH);
			}
		}

		final String key = generateKey(uuid, id);
		CompletableFuture<Packet> future = new CompletableFuture<>();
		// queue callback if we need result
		if (timeout == null || timeout < 0) {
			timeout = 30;
		}

		pendingRequest.put(key, future);
		if (addOutPacket(packet)) {
			return future.orTimeout(timeout, TimeUnit.SECONDS).whenComplete((a,b) -> {
				pendingRequest.remove(key);
			});
		} else {
			pendingRequest.remove(key);
			return CompletableFuture.failedFuture(new ComponentException(Authorization.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void register(Kernel kernel) {
	}

	@Override
	public void unregister(Kernel kernel) {
	}

	/**
	 * Generates key for setting use in pending requests map
	 *
	 * @param uuid
	 * @param key
	 *
	 * @return
	 */
	private String generateKey(String uuid, String key) {
		return uuid + "-" + key;
	}

	private boolean processResultPacket(Packet packet) {
		String uuid = packet.getTo().getResource();
		String id = packet.getAttributeStaticStr("id");
		String key = generateKey(uuid, id);

		// check if we send this request and if we are waiting for response process it
		CompletableFuture<Packet> future = pendingRequest.remove(key);
		if (future != null) {
			future.complete(packet);
			return true;
		}

		return false;
	}

}
