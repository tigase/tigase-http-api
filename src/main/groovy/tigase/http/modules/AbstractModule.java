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
package tigase.http.modules;

import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.disco.ServiceEntity;
import tigase.http.CommandManager;
import tigase.http.PacketWriter;
import tigase.http.PacketWriter.Callback;
import tigase.http.api.HttpServerIfc;
import tigase.http.modules.rest.ApiKeyRepository;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.script.CommandIfc;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import javax.script.Bindings;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractModule implements Module, Initializable, ConfigurationChangedAware, UnregisterAware {

	private static final ConcurrentHashMap<String,AbstractModule> modules = new ConcurrentHashMap<>();

	@Inject
	protected HttpServerIfc httpServer = null;

	@Inject
	private UserRepository userRepository;

	@Inject
	private AuthRepository authRepository;

	@ConfigField(desc = "Module name")
	protected String name;
	private JID jid;
	private PacketWriter writer;
	
	private ServiceEntity serviceEntity = null;
	@Inject
	private ApiKeyRepository apiKeyRepository;
	protected CommandManager commandManager = new CommandManager(this);

	private String componentName;
	protected final String uuid = UUID.randomUUID().toString();
	
	public static <T extends AbstractModule> T getModuleByUUID(String uuid) {
		return (T) modules.get(uuid);
	}


	@ConfigField(desc = "Context path", alias = HTTP_CONTEXT_PATH_KEY)
	protected String contextPath = null;
	@ConfigField(desc = "List of vhosts", alias = VHOSTS_KEY)
	protected String[] vhosts = null;

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		contextPath = "/" + name;
	}

	@Override
	public boolean addOutPacket(Packet packet) {
		return writer.write(this, packet);
	}

	@Override
	public boolean addOutPacket(Packet packet, Integer timeout, Callback callback){		
		return writer.write(this, packet, timeout, callback);
	}

	@Override
	public void everyHour() {}
	@Override
	public void everyMinute() {}
	@Override
	public void everySecond() {}
	
	@Override
	public String[] getFeatures() {
		return new String[] { Command.XMLNS };
	}
	
	@Override
	public boolean processPacket(Packet packet) {
		if (packet.isCommand()) {
			return commandManager.execute(packet);
		}
		return false;
	}
	
	@Override
	public Element getDiscoInfo(String node, boolean isAdmin) {
		return serviceEntity.getDiscoInfo(node, isAdmin);
	}

	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {
		if (jid.getLocalpart() == null) {
			return Collections.singletonList(serviceEntity.getDiscoItem(node, getName() + "@" + jid.toString()));
		}
		else {
			if (node != null) {
				if (node.equals("http://jabber.org/protocol/commands") && this.isAdmin(from.getBareJID())) {
					List<Element> result = new LinkedList<Element>();
					for (CommandIfc comm : commandManager.getCommands()) {
						result.add(new Element("item", new String[] { "node", "name", "jid" },
								new String[] { comm.getCommandId(),
								comm.getDescription(), jid.toString() }));
					}
					return result;
				} 
			}
			return serviceEntity.getItems(node, jid.toString());
		}
	}

	@Override
	public JID getJid() {
		return jid;
	}
	
	@Override
	public void getStatistics(String compName, StatisticsList list) {
		
	}
	
	@Override
	public void setStatisticsPrefix(String prefix) {
		
	}

	public void setApiKeyRepository(ApiKeyRepository apiKeyRepository) {
		if (componentName != null) {
			apiKeyRepository.setRepoUser(BareJID.bareJIDInstanceNS(getName(), componentName));
			apiKeyRepository.setRepo(userRepository);
		}
		this.apiKeyRepository = apiKeyRepository;
	}

	@Override
	public void statisticExecutedIn(long executionTime) {
		
	}
	
	@Override
	public void init(JID jid, String componentName, PacketWriter writer) {
		this.componentName = componentName;
		this.jid = JID.jidInstanceNS(getName(), jid.getDomain(), null);
		this.writer = writer;
	}
	
	@Override
	public void initBindings(Bindings binds) {
		binds.put("module", this);
	}
	
	@Override
	public boolean isAdmin(BareJID user) {
		return writer.isAdmin(JID.jidInstance(user));
	}
	
	@Override
	public boolean isRequestAllowed(String key, String domain, String path) {
		return apiKeyRepository.isAllowed(key, domain, path);
	}
	
	@Override
	public UserRepository getUserRepository() {
		return userRepository;
	}
	
	@Override
	public AuthRepository getAuthRepository() {
		return authRepository;
	}
	
	@Override
	public void start() {
		modules.put(uuid, this);
	}
	
	@Override
	public void stop() {
		modules.remove(uuid, this);
		apiKeyRepository.setAutoloadTimer(0);
	}

	public void executedIn(String path, long executionTime) {
		
	}

	@Override
	public void initialize() {
		serviceEntity = new ServiceEntity(getName(), null, getDescription(), true);
		serviceEntity.setFeatures(getFeatures());
		start();
	}

	@Override
	public void beforeUnregister() {
		stop();
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		if (httpServer == null)
			return;

		start();
	}
}
