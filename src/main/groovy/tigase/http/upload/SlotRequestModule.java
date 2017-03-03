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

import tigase.component.PacketWriter;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.Module;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.http.upload.logic.Logic;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.Authorization;

/**
 * Created by andrzej on 06.08.2016.
 */
@Bean(name = "slotRequestModule", parent = FileUploadComponent.class, active = true)
public class SlotRequestModule implements Module {

	private static final String XMLNS = "urn:xmpp:http:upload";

	private static final Criteria CRITERIA = ElementCriteria.name("iq").add(ElementCriteria.name("request", XMLNS));
	private static final String[] FEATURES = { XMLNS };

	@Inject
	private Logic logic;

	@Inject
	private PacketWriter packetWriter;

	@Override
	public String[] getFeatures() {
		return FEATURES;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRITERIA;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		String filename;
		long size;
		String contentType;

		try {
			Element request = packet.getElement().getChild("request", XMLNS);

			filename = XMLUtils.unescape(request.getChild("filename").getCData());
			size = Long.parseLong(request.getChild("size").getCData());
			contentType = request.getChild("content-type").getCData();
		} catch (NullPointerException ex) {
			throw new ComponentException(Authorization.BAD_REQUEST, null, ex);
		}

		if (size <= 0)
			throw new ComponentException(Authorization.BAD_REQUEST, "Invalid file size");

		String slotId = logic.requestSlot(packet.getStanzaFrom(), filename, size, contentType);

		String uploadURI = logic.getUploadURI(packet.getStanzaFrom(),slotId, filename);
		String downloadURI = logic.getDownloadURI(packet.getStanzaFrom(), slotId, filename);

		Element slot = new Element("slot");
		slot.setXMLNS(XMLNS);
		slot.addChild(new Element("put", XMLUtils.escape(uploadURI)));
		slot.addChild(new Element("get", XMLUtils.escape(downloadURI)));

		Packet result = packet.okResult(slot, 0);

		packetWriter.write(result);
	}
}
