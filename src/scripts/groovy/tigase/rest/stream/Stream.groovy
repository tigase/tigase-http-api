/*
 * Tigase HTTP API
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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

package rest.stream

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import tigase.http.rest.Service
import tigase.server.Packet
import tigase.xmpp.BareJID
import tigase.xml.DomBuilderHandler
import tigase.xml.SimpleParser
import tigase.xml.Element
import tigase.xml.SingletonFactory

/**
 * Class implements ability to send packets to any JID using REST API
 *
 * @author andrzej
 */
class Stream extends tigase.http.rest.Handler {
	
	public Stream() {
		regex = /\/(.*)/
		isAsync = true
		decodeContent = false
		requiredRole = "admin"
		
		execPost = { Service service, callback, user, HttpServletRequest request, to ->
			char[] data = request.getReader().getText()?.toCharArray();
			if (data == null || data.length == 0) {
				callback({ req, HttpServletResponse resp ->
						resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
				})
				return;
			}
			
			SimpleParser parser   = SingletonFactory.getParserInstance();
			DomBuilderHandler domHandler = new DomBuilderHandler();
			parser.parse(domHandler, data, 0, data.length);			
			Element packetEl = domHandler.getParsedElements().poll();
			if (packetEl == null) {
				callback({ req, HttpServletResponse resp ->
						resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
				})
				return;				
			}
			
			if (to != null) {
				to = to.trim();
				if (!to.isEmpty()) {
					packetEl.setAttribute("to", to);
				}
			}
			
			Packet packet = Packet.packetInstance(packetEl);
			def responseHandler = (packet.getElemName() == "iq" && packet.getAttribute("from") == null) ? { Packet result ->
				callback({ req, HttpServletResponse resp -> 
					def outBytes = result.getElement().toString().getBytes();
					resp.setContentType("application/xml");
					resp.setContentLength(outBytes.length);
					resp.getOutputStream().write(outBytes);
				});
			} : null;
			
			service.sendPacket(packet, null, responseHandler);
			
			if (responseHandler == null) {
				callback("");
			}
		}
	}
}
