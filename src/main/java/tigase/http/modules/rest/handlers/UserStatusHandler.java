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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.XMPPServer;
import tigase.util.Algorithms;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Bean(name = "userStatus", parent = RestModule.class, active = true)
@Path("/user")
public class UserStatusHandler extends AbstractRestHandler {

	@Inject
	private RestModule module;

	public UserStatusHandler() {
		super(Security.ApiKey, Role.None);
	}

	public RestModule getModule() {
		return module;
	}

	public void setModule(RestModule module) {
		this.module = module;
	}
	
	@POST
	@Path("/{jid}/status")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	@Operation(summary = "Set user status", description = "Set user connection status")
	@ApiResponse(responseCode = "200", description = "If set correctly", content = @Content(schema = @Schema(implementation = UserStatus.class)))
	public void setStatus(@Parameter(description = "JID of the user") @NotNull @PathParam("jid") JID userJid, @NotNull UserStatus status, @Suspended AsyncResponse asyncResponse) {
		JID jid = userJid.getResource() != null ? userJid : userJid.copyWithResourceNS("tigase-external");
		Packet packet = Command.USER_STATUS.getPacket(
				JID.jidInstanceNS("user-status-endpoint", getModule().getJid().getDomain(),
								  Algorithms.sha256(userJid.toString())),
				JID.jidInstanceNS("sess-man", getModule().getJid().getDomain()), StanzaType.set,
				UUID.randomUUID().toString(), Command.DataType.submit);

		Command.addFieldValue(packet, "jid", jid.toString());
		Command.addFieldValue(packet, "available", String.valueOf(status.isAvailable()));

		Element presence = new Element("presence").withElement("priority", null, String.valueOf(status.priority))
				.withElement("c", "http://jabber.org/protocol/caps", c -> {
					c.setAttribute("node", "http://www.google.com/xmpp/client/caps");
					c.setAttribute("ver", XMPPServer.getImplementationVersion());
					c.setAttribute("ext", "voice-v1");
				});

		Optional.ofNullable(status.getShow())
				.map(UserStatus.Show::name)
				.map(XMLUtils::escape)
				.ifPresent(show -> presence.withElement("show", null, show));
		Optional.ofNullable(status.getStatus())
				.filter(str -> !str.isEmpty())
				.ifPresent(statusStr -> presence.withElement("status", null, statusStr));

		for (UserStatus.Activity activity : Optional.ofNullable(status.getActivities()).orElse(Collections.emptyList())) {
			presence.withElement("activity", "http://jabber.org/protocol/activity", el -> {
				el.withElement(activity.getCategory(), null, categoryEl -> {
					Optional.ofNullable(activity.getType())
							.filter(str -> !str.isEmpty())
							.ifPresent(type -> categoryEl.withElement(activity.getType(), null, (String) null));
				});
				Optional.ofNullable(activity.getText())
						.filter(str -> !str.isEmpty())
						.map(XMLUtils::escape)
						.ifPresent(text -> el.withElement("text", null, text));
			});
		}

		packet.getElement().getChild("command").addChild(presence);

		sendResult(getModule().sendPacketAndWait(packet).thenApply(result -> status), asyncResponse);
	}

	@XmlRootElement(name = "status")
	public static class UserStatus {

		public enum Show {
			away, chat, dnd, xa
		}

		@XmlAttribute
		private boolean available = true;
		@XmlAttribute
		private int priority = -1;
		@XmlAttribute
		private Show show;
		@XmlElement
		private String status;
		@XmlElement(name = "activity")
		private List<Activity> activities;

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		public int getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public Show getShow() {
			return show;
		}

		public void setShow(Show show) {
			this.show = show;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public List<Activity> getActivities() {
			return activities;
		}

		public void setActivities(List<Activity> activities) {
			this.activities = activities;
		}

		@XmlRootElement(name = "activity")
		public static class Activity {
			@NotNull
			@XmlAttribute
			private String category;
			@XmlAttribute
			private String type;
			@XmlValue
			private String text;

			public String getCategory() {
				return category;
			}

			public void setCategory(String category) {
				this.category = category;
			}

			public String getType() {
				return type;
			}

			public void setType(String type) {
				this.type = type;
			}

			public String getText() {
				return text;
			}

			public void setText(String text) {
				this.text = text;
			}
		}
	}

}
