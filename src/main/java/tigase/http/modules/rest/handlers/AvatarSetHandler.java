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
package tigase.http.modules.rest.handlers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.util.Base64;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.jid.BareJID;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Bean(name = "avatarPut", parent = RestModule.class, active = true)
@Path("/avatar")
public class AvatarSetHandler extends AbstractRestHandler {

	@Inject
	private UserRepository userRepository;

	public AvatarSetHandler() {
		super(RestHandler.Security.ApiKey, RestHandler.Role.None);
	}

	@PUT
	@Path("/{userJid}")
	@Operation(summary = "Set user avatar", description = "Set user avatar")
	@ApiResponse(responseCode = "200", description = "If set correctly")
	public Response setAvatar(
			@Parameter(description = "Bare JID of the user") @NotNull @PathParam("userJid") BareJID user,
			@Parameter(description = "Content type of content (avatar data)") @NotNull @HeaderParam("Content-Type") String contentType,
			@Parameter(description = "Binary form of the avatar") byte[] data) throws TigaseDBException {
		String base64EncodedPhoto = Base64.encode(data);
		Element vCard = Optional.ofNullable(getVCard(user)).orElseGet(() -> createEmptyVCard());
		String mimeType = contentType.split(";")[0];

		Element photo = vCard.getChild("PHOTO");
		if (photo != null) {
			vCard.removeChild(photo);
		}

		photo = new Element("PHOTO").withElement("TYPE", null, mimeType)
				.withElement("BINVAL", null, base64EncodedPhoto);
		vCard.addChild(photo);

		setVCard(user, vCard);

		return Response.ok().build();
	}

	private Element getVCard(BareJID user) throws TigaseDBException {
		String str = userRepository.getData(user, "public/vcard-temp", "vCard", null);
		if (str == null) {
			return null;
		}

		SimpleParser parser = SingletonFactory.getParserInstance();
		DomBuilderHandler domHandler = new DomBuilderHandler();
		char[] vCardData = str.toCharArray();
		parser.parse(domHandler, vCardData, 0, vCardData.length);
		return domHandler.getParsedElements().poll();
	}

	private void setVCard(BareJID user, Element vcard) throws TigaseDBException {
		userRepository.setData(user, "public/vcard-temp", "vCard", vcard.toString());
	}

	private Element createEmptyVCard() {
		return new Element("vCard").withAttribute("xmlns", "vcard-temp");
	}

}
