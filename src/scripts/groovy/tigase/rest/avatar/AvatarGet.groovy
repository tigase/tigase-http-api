package tigase.rest.avatar
/*
 * Tigase HTTP API
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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
import tigase.http.rest.Service
import tigase.server.Iq
import tigase.util.Base64
import tigase.xml.Element
import tigase.xmpp.StanzaType

class AvatarHandler extends tigase.http.rest.Handler {

    public AvatarHandler() {
        regex = /\/([^@\/]+)@([^@\/]+)/
        isAsync = true
        execGet = { Service service, callback, localPart, domain ->

            Element iq = new Element("iq");
            iq.setAttribute("to", "$localPart@$domain");
            iq.setAttribute("type", "get");

            Element vcard = new Element("vCard");
            vcard.setXMLNS("vcard-temp");
            iq.addChild(vcard);

            service.sendPacket(new Iq(iq), 30, { result ->
                if (result == null || result.getType() == StanzaType.error) {
                    callback(null);
                    return;
                }

                def photo = result.getElement().getChildren().find { it.getName() == "vCard" && it.getXMLNS() == "vcard-temp" }?.getChildren().find { it.getName() == "PHOTO" };
                if (photo == null) {
                    callback(null);
                    return;
                }

                def outResult = new tigase.http.rest.Handler.Result();
                outResult.contentType = photo.getChildren().find { it.getName() == "TYPE" }?.getCData()
                String contentBase64 = photo.getChildren().find { it.getName() == "BINVAL" }?.getCData();
                outResult.data = Base64.decode(contentBase64);

                callback(outResult);
            });
        }
    }

}