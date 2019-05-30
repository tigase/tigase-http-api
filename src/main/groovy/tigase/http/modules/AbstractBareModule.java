/**
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
package tigase.http.modules;

import tigase.disco.ServiceEntity;
import tigase.http.AbstractHttpModule;
import tigase.http.CommandManager;
import tigase.http.PacketWriter;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.script.CommandIfc;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.script.Bindings;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by andrzej on 28.03.2017.
 */
public abstract class AbstractBareModule
		extends AbstractHttpModule
		implements Module, Initializable, ConfigurationChangedAware, UnregisterAware {

	private static final ConcurrentHashMap<String, AbstractBareModule> modules = new ConcurrentHashMap<>();
	protected CommandManager commandManager = new CommandManager(this);
	@ConfigField(desc = "Module name")
	protected String name;
	private String componentName;
	private JID jid;
	private ServiceEntity serviceEntity = null;
	private PacketWriter writer;

	public static <T extends Module> T getModuleByUUID(String uuid) {
		return (T) modules.get(uuid);
	}

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
	public boolean addOutPacket(Packet packet, Integer timeout, PacketWriter.Callback callback) {
		return writer.write(this, packet, timeout, callback);
	}

	@Override
	public void everyHour() {
	}

	@Override
	public void everyMinute() {
	}

	@Override
	public void everySecond() {
	}

	@Override
	public String[] getFeatures() {
		return new String[]{Command.XMLNS};
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
		} else {
			if (node != null) {
				if (node.equals("http://jabber.org/protocol/commands") && this.isAdmin(from.getBareJID())) {
					List<Element> result = new LinkedList<Element>();
					for (CommandIfc comm : commandManager.getCommands()) {
						Element item = new Element("item", new String[]{"node", "name", "jid"},
												 new String[]{comm.getCommandId(), comm.getDescription(),
															  jid.toString()});
						if (comm.getGroup() != null) {
							item.setAttribute("group", comm.getGroup());
						}
						result.add(item);
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
	public void start() {
		modules.put(uuid, this);
	}

	@Override
	public void stop() {
		modules.remove(uuid, this);
	}

	public void executedIn(String path, long executionTime) {

	}

	@Override
	public void initialize() {
		super.initialize();
		serviceEntity = new ServiceEntity(getName(), null, getDescription(), null,true);
		serviceEntity.setFeatures(getFeatures());
	}

	protected String getComponentName() {
		return componentName;
	}
}
