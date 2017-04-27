/*
 * Tigase HTTP API
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.http;

import tigase.http.api.HttpServerIfc;
import tigase.http.modules.Module;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "http", parent = Kernel.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SetupMode})
public class HttpMessageReceiver extends AbstractMessageReceiver implements PacketWriter, RegistrarBean {

	private static final Logger log = Logger.getLogger(HttpMessageReceiver.class.getCanonicalName());
	
    private ScheduledExecutorService scheduler;
    private ConcurrentHashMap<String,Request> pendingRequest = new ConcurrentHashMap<String,Request>();

	private Map<String,Module> modules = new ConcurrentHashMap<String,Module>();
	@Inject
	private List<Module> activeModules = new ArrayList<>();

	@Inject
	private HttpServerIfc httpServer;
	
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
		}
		else {			
			Module module = modules.get(jid.getLocalpart());
			if (module != null) {
				return module.getDiscoInfo(node, isAdmin(from));
			}
			else {
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
				}
				else {
					return Collections.emptyList();
				}
			}
			else {				
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
				}
				else
					return super.getDiscoItems(node, jid, from);
			}
			//return getServiceEntity().getDiscoItems(node, jid.toString());
		}
		else{
			return super.getDiscoItems(node, jid, from);
		}
	}	
	
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);

		for (Module m : modules.values()) {
			m.getStatistics(getName(), list);
		}
	}

	@Override
	public void processPacket(Packet packet) {
		boolean handled = false;
		
		if (packet.getType() == StanzaType.result || packet.getType() == StanzaType.error) {
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
			try {
				// we can only process response we are waiting for so return error if packet is not expected
				addOutPacket(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, false));
			} catch (PacketErrorTypeException ex) {
				log.log(Level.FINEST, "packet processing type error", ex);
			}
		}		
	}

    public void requestTimedOut(String key) {
        Request request = pendingRequest.remove(key);
        if (request == null) return;

        request.callback.onResult(null);
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
	public boolean write(Module module, Packet packet, Integer timeout, Callback callback) {
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
            }
            else {
                packet.setPermissions(Permissions.AUTH);
            }
        }

        final String key = generateKey(uuid, id);

        if (callback != null) {
            // queue callback if we need result
            if (timeout == null || timeout < 0) timeout = 30;

            Request request = new Request(callback, scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					requestTimedOut(key);
				}
            }, timeout, TimeUnit.SECONDS));

            pendingRequest.put(key, request);
        }

        // send packet		
		return addOutPacket(packet);
	}

    /**
     * Generates key for setting use in pending requests map
     *
     * @param uuid
     * @param key
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
        Request request = pendingRequest.remove(key);
        if (request != null) {
            request.future.cancel(false);
			try {
				request.callback.onResult(packet);
			} catch (IllegalStateException ex) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "exception while processing response on HTTP request,"
							+ " is HTTP connection closed?", ex);
				}
			}
        }
		
		return (request != null);
	}

	@Override
	public void register(Kernel kernel) {
	}

	@Override
	public void unregister(Kernel kernel) {
	}

	private class Request {
        final Future future;
        final Callback callback;
		
		Request(Callback callback, Future future) {
			this.callback = callback;
			this.future = future;
		}
    }	
}
