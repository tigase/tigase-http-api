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
package tigase.http.modules.dashboard;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import tigase.http.api.HttpException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.util.Algorithms;
import tigase.xmpp.jid.BareJID;

import javax.servlet.http.Part;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "userAvatar", parent = DashboardModule.class, active = true)
@Path("/users")
public class UserAvatarHandler extends DashboardHandler {

	private static final Logger log = Logger.getLogger(UserAvatarHandler.class.getCanonicalName());

	@Inject
	private UserAvatarRepository avatarRepository;
	@Inject
	private PermissionsHelper permissionsHelper;

	public UserAvatarHandler() {
		super();
	}

	@Override
	public Role getRequiredRole() {
		return Role.User;
	}

	@GET
	@Path("/{user}/avatar/{id}")
	public void getAvatar(@PathParam("user") BareJID userJid, @PathParam("id") String id, @Suspended AsyncResponse response) {
		avatarRepository.getMetadata(userJid).thenCombine(avatarRepository.getData(userJid, id), (metadata, data) -> {
			try {
				if (metadata == null || data == null) {
					response.resume(Response.status(HttpStatus.NOT_FOUND_404));
				} else {
					response.resume(jakarta.ws.rs.core.Response.ok(data, metadata.mimeType())
											.expires(Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneOffset.systemDefault()).toInstant()))
											.build());
				}
			} catch (Throwable ex) {
				log.log(Level.WARNING, ex, () -> "failed to send avatar for user " + userJid + " with id " + id);
				response.resume(ex);
			}
			return null;
		});
	}

	@POST
	@Path(value = "/{user}/avatar")
	@RolesAllowed({"admin", "account_manager", "user"})
	public void setAvatar(@PathParam("user") BareJID userJid, @FormParam("file") Part filePart, @Suspended AsyncResponse response) {
		try {
			if (!permissionsHelper.canManageUser(userJid)) {
				throw new HttpException("Forbidden", 403);
			}
			log.finest(() -> "setting avatar for user " + userJid + " to " + filePart.getContentType() + " of " +
					filePart.getSize());
			byte[] data = filePart.getInputStream().readAllBytes();
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(data);
			String id = Algorithms.bytesToHex(md.digest());
			avatarRepository.setData(userJid, id, data).thenCompose(r -> {
				return avatarRepository.setMetadata(userJid, new UserAvatarRepository.AvatarMetadata(id, filePart.getContentType(), data.length, null));
			}).whenComplete((r, ex) -> {
				if (ex == null) {
					response.resume(Response.ok().build());
				} else {
					response.resume(ex);
				}
			});
		} catch (Throwable ex) {
			response.resume(ex);
		}
	}

	@DELETE
	@Path(value = "/{user}/avatar")
	@RolesAllowed({"admin", "account_manager", "user"})
	public void deleteAvatar(@PathParam("user") BareJID userJid, @Suspended AsyncResponse response) {
		if (!permissionsHelper.canManageUser(userJid)) {
			response.resume(new HttpException("Forbidden", 403));
			return;
		}
		avatarRepository.getMetadata(userJid).thenCompose(metadata -> {
			if (metadata != null) {
				return avatarRepository.deleteMetadata(userJid);
			} else {
				return CompletableFuture.completedFuture(null);
			}
		}).whenComplete((r, ex) -> {
			if (ex == null) {
				response.resume(Response.ok().build());
			} else {
				response.resume(ex);
			}
		});
	}
}
