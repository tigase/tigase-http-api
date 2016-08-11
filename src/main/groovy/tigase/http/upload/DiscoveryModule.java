/*
 * Tigase HTTP API
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.upload;

import tigase.kernel.beans.Bean;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.JID;

/**
 * Created by andrzej on 06.08.2016.
 */
@Bean(name = DiscoveryModule.ID, parent = FileUploadComponent.class)
public class DiscoveryModule extends tigase.component.modules.impl.DiscoveryModule {

	private static final String XMLNS = "urn:xmpp:http:upload";

	protected Packet prepareDiscoInfoReponse(Packet packet, String jid, String node, JID senderJID) {
		Packet result = prepareDiscoInfoReponse(packet, jid, node, senderJID);

		Element fileUploadForm = getForm();

		if (fileUploadForm != null) {
			result.getElement().getChild("query", DISCO_INFO_XMLNS).addChild(fileUploadForm);
		}

		return result;
	}

	protected Element getForm() {
		Element x = new Element("x", new String[] { "type", "xmlns" }, new String[] { "result", "jabber:x:data" });

		Element formType = new Element("field");
		formType.setAttribute("var", "FORM_TYPE");
		formType.addChild(new Element("value", XMLNS));

		x.addChild(formType);

		Element maxFileSize = new Element("field");
		maxFileSize.setAttribute("var", "max-file-size");
		maxFileSize.addChild(new Element("value", String.valueOf(getMaxFileSize())));

		x.addChild(maxFileSize);

		return x;
	}

	// TODO: Implement this properly!
	private long getMaxFileSize() {
		return -1;
	}
}
