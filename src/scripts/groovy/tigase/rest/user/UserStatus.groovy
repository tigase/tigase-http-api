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
package rest.user

import tigase.http.rest.Service
import tigase.server.Command
import tigase.server.Packet
import tigase.server.XMPPServer
import tigase.util.Algorithms
import tigase.util.DNSResolverFactory
import tigase.xml.Element
import tigase.xml.XMLUtils
import tigase.xmpp.JID
import tigase.xmpp.StanzaType
/**
 * Class implements ability to change user status
 */
class UserStatusHandler
		extends tigase.http.rest.Handler {

	public UserStatusHandler() {
		description = [ regex : "/{user_jid}/status/{resource}",
						POST  : [ info       : 'Set user status',
								  description: """Part of url {user_jid} is parameter which is jid of user which status you want to set and {resource} is optional resource of the jid.\n\
Additional data needs to be passed as content of HTTP request:
\${util.formatData([available: 'true', priority: '-1', show: 'xa', status: 'On the phone'])}
Data will be returned in form of JSON or XML depending on selected format by Accept HTTP header

Example response:
\${util.formatData([available: 'true', priority: '-1', show: 'xa', status: 'On the phone'])}				
""" ] ];
		regex = /\/([^@\/]+)@([^@\/]+)\/status(\/[^@\/]+)?/
		authRequired = { api_key -> return api_key == null && requiredRole != null }
		requiredRole = "admin"
		isAsync = true
		execPost = { Service service, callback, user, content, localPart, domain, resourcePart ->
			String resource = resourcePart?.substring(1);
			if (resource == null || resource.isEmpty()) {
				resource = "tigase-external";
			}
			String userJid = "${localPart}@${domain}/${resource}";
			String available = content.available ?: "true";
			String priority = content.priority ?: "-1";
			String show = content.show;
			String status = content.status;

			String hash = Algorithms.sha256(userJid);

			Packet packet = Command.USER_STATUS.getPacket(JID.jidInstanceNS("user-status-endpoint", service.module.jid.getDomain(), hash),
														  JID.jidInstanceNS("sess-man", DNSResolverFactory.getInstance().getDefaultHost()), StanzaType.set,
														  UUID.randomUUID().toString(), Command.DataType.submit);

			Command.addFieldValue(packet, "jid", userJid);
			Command.addFieldValue(packet, "available", available);

			Element presence = new Element("presence");
			presence.addChild(new Element("priority", priority));
			presence.addChild(new Element("c", ["node", "ver", "ext", "xmlns"] as String[],
										  ["http://www.google.com/xmpp/client/caps",
										   XMPPServer.getImplementationVersion(), "voice-v1",
										   "http://jabber.org/protocol/caps"] as String[]));
			if (show != null && !show.isEmpty()) {
				presence.addChild(new Element("show", XMLUtils.escape(show)));
			}
			if (status != null && !status.isEmpty()) {
				presence.addChild(new Element("status", XMLUtils.escape(status)));
			}

			packet.getElement().getChild("command").addChild(presence);

			service.sendPacket(packet, 5 * 1000, { result ->
				boolean ok = result != null && result.getType() == StanzaType.result;
				callback([ status: [ user: userJid, available: available, priority: priority, show: show, status: status, success: ok] ]);
			});
		}
	}

}

