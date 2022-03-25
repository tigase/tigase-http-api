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
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import tigase.http.api.HttpException;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.http.util.XmppException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

@Path("/stream")
@Bean(name = "stream", parent = RestModule.class, active = true)
public class StreamHandler extends AbstractRestHandler {

	@Inject
	private RestModule module;
	
	public StreamHandler() {
		super(Security.ApiKey, Role.None);
	}

	public RestModule getModule() {
		return module;
	}

	public void setModule(RestModule module) {
		this.module = module;
	}

	@POST
	@Path("/{toJid}")
	@Consumes({"application/xml"})
	@Produces({"application/xml"})
	@Operation(summary = "Send XML stanza", description = "Sends provided XML stanza to provided recipient and in case of <iq/> awaits for response which it returns")
	@ApiResponse(responseCode = "200", description = "Received response or nothing")
	@ApiResponse(responseCode = "400", description = "Provided content was unacceptable")
	public void sendStanza(@Parameter(description = "Destination JID to send the stanza to") @NotNull @PathParam("toJid") JID destination, @Parameter(description = "XML payload to send") @NotNull Element stanza, @Suspended
						   AsyncResponse asyncResponse) {
		String toAttr = stanza.getAttributeStaticStr("to");
		if (toAttr != null && !destination.toString().equals(toAttr)) {
			asyncResponse.resume(new HttpException("Destination does not match 'to' attribute value!",
												   HttpServletResponse.SC_BAD_REQUEST));
			return;
		} else {
			stanza.setAttribute("to", destination.toString());
		}

		try {
			Packet packet = Packet.packetInstance(stanza);
			if (packet.getElemName().equals("iq") && packet.getStanzaFrom() == null) {
				getModule().sendPacketAndWait(packet).thenApply(Packet::getElement).thenAccept(asyncResponse::resume).exceptionally( ex -> {
					if (ex instanceof XmppException) {
						asyncResponse.resume(((XmppException) ex).getStanza());
					} else {
						asyncResponse.resume(ex);
					}
					return null;
				});
			} else {
				if (getModule().sendPacket(packet)) {
					asyncResponse.resume(null);
				} else {
					asyncResponse.resume(new HttpException("Internal Server Error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
				}
			}
		} catch (TigaseStringprepException e) {
			asyncResponse.resume(new HttpException(e.getMessage(), HttpServletResponse.SC_BAD_REQUEST));
			return;
		}
	}

}
