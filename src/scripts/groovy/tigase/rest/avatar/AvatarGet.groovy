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

/**
 * Class implements retrieving user avatar from VCard
 * HTTP GET request for /rest/avatar/user@domain will return avatar for jid user@domain retrived from VCard
 */
class AvatarHandler extends tigase.http.rest.Handler {

    public AvatarHandler() {
        regex = /\/(?:([^@\/]+)@){0,1}([^@\/]+)/
        isAsync = true
        execGet = { Service service, callback, localPart, domain ->

            Element iq = new Element("iq");
            iq.setAttribute("to", localPart != null ? "$localPart@$domain" : domain);
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
                // TODO: added workaround for bad result of Base64.decode when encoded data is wrapped
                // It should be removed when issue https://projects.tigase.org/issues/1265 is fixed
                // outResult.data = Base64.decode(contentBase64);
                outResult.data = Base64.decode(contentBase64.replace('\n', '').replace('\r',''));

                callback(outResult);
            });
        }
    }

}