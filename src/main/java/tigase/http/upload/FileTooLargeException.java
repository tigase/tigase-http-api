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
package tigase.http.upload;

import tigase.component.exceptions.ComponentException;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

public class FileTooLargeException extends ComponentException {

	private final long limit;

	public FileTooLargeException(long limit) {
		super(Authorization.NOT_ACCEPTABLE, "File too large. The maximum file size is " + limit + " bytes");
		this.limit = limit;
	}

	@Override
	public Packet makeElement(Packet packet, boolean insertOriginal) throws PacketErrorTypeException {
		Packet result =  super.makeElement(packet, insertOriginal);
		Element error = result.getElemChild("error");
		if (error != null) {
			Element fileTooLargeEl = new Element("file-too-large");
			fileTooLargeEl.setXMLNS("urn:xmpp:http:upload:0");
			fileTooLargeEl.addChild(new Element("max-file-size", String.valueOf(limit)));
			error.addChild(fileTooLargeEl);
		}
		return result;
	}
}
