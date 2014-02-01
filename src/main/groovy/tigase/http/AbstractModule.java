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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.script.Bindings;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.disco.ServiceEntity;
import tigase.http.PacketWriter.Callback;
import tigase.http.rest.ApiKeyRepository;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.script.CommandIfc;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

public abstract class AbstractModule implements Module {

	private JID jid;
	private PacketWriter writer;
	
	private ServiceEntity serviceEntity = null;
	protected CommandManager commandManager = new CommandManager(this);
	
	@Override
	public boolean addOutPacket(Packet packet) {
		return writer.write(this, packet);
	}

	@Override
	public boolean addOutPacket(Packet packet, Integer timeout, Callback callback){		
		return writer.write(this, packet, timeout, callback);
	}

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
	public Map<String, Object> getDefaults() {
		Map<String,Object> props = new HashMap<String,Object>();
		props.put("active", true);
		return props;
	}

	@Override
	public JID getJid() {
		return jid;
	}
	
	@Override
	public void setProperties(Map<String, Object> props) {
		serviceEntity = new ServiceEntity(getName(), null, getDescription(), true);
		serviceEntity.setFeatures(getFeatures());
	}
	
	@Override
	public void init(JID jid, PacketWriter writer) {
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
		return writer.getApiKeyRepository().isAllowed(key, domain, path);
	}
	
	@Override
	public UserRepository getUserRepository() {
		return writer.getUserRepository();
	}
	
	@Override
	public AuthRepository getAuthRepository() {
		return writer.getAuthRepository();
	}
	
}
