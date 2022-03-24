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
package tigase.http.util;

import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;

import java.util.Optional;

public class XmppException extends Exception {

	public static XmppException fromStanza(Packet packet) {
		if (packet.getType() != StanzaType.error) {
			return null;
		} else {
			Optional<Element> error = Optional.ofNullable(packet.getElemChild("error"));
			Authorization authorization = error.map(
							err -> err.findChild(it -> it.getXMLNS() == "urn:ietf:params:xml:ns:xmpp-stanzas"))
					.map(Element::getName)
					.map(Authorization::getByCondition)
					.orElse(Authorization.UNDEFINED_CONDITION);
			String text = error.map(err -> err.getChild("text", "urn:ietf:params:xml:ns:xmpp-stanzas"))
					.map(Element::getCData)
					.map(XMLUtils::unescape)
					.orElse(null);
			return new XmppException(text, authorization, packet.getElement().clone());
		}
	}

	private final Authorization authorization;
	private final Element stanza;

	public XmppException(String message, Authorization authorization, Element stanza) {
		super(message);
		this.authorization = authorization;
		this.stanza = stanza;
	}

	public Authorization getAuthorization() {
		return authorization;
	}

	public int getCode() {
		return authorization.getErrorCode();
	}

	public String getType() {
		return authorization.getErrorType();
	}

	public Element getStanza() {
		return stanza;
	}

}
