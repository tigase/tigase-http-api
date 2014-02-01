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

import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.server.Packet;
import tigase.http.PacketWriter.Callback;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

public interface Module {
	
	public static final String VHOSTS_KEY = "vhosts";
	public static final String HTTP_SERVER_KEY = "http-server";
	public static final String HTTP_CONTEXT_PATH_KEY = "context-path";
	
	String getName();
	
	String getDescription();
	
	Element getDiscoInfo(String node, boolean isAdmin);
	
	List<Element> getDiscoItems(String node, JID jid, JID from);
	
	JID getJid();
	
	boolean addOutPacket(Packet packet);
	
	boolean addOutPacket(Packet packet, Integer timeout, Callback callback);
	
	String[] getFeatures();
	
	void initBindings(Bindings binds);
	
	boolean processPacket(Packet packet);
	
	Map<String,Object> getDefaults();
	
	void setProperties(Map<String,Object> props);
	
	void init(JID jid, PacketWriter writer);
	
	boolean isRequestAllowed(String key, String domain, String path);
	
	boolean isAdmin(BareJID user);
	
	void start();
	
	void stop();
	
	UserRepository getUserRepository();
	
	AuthRepository getAuthRepository();
	
}
