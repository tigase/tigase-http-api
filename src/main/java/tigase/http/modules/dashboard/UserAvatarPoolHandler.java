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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import tigase.db.TigaseDBException;
import tigase.http.api.HttpException;
import tigase.http.jaxrs.Handler;
import tigase.http.jaxrs.Model;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.util.Algorithms;
import tigase.xmpp.jid.BareJID;

import javax.servlet.http.Part;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static tigase.db.DataSource.log;

@Bean(name = "usersAvatarPoolHandler", parent = DashboardModule.class, active = true)
@Path("/usersAvatarPool")
public class UserAvatarPoolHandler extends DashboardHandler {

	@Inject
	private PermissionsHelper permissionsHelper;
	@Inject
	private UserAvatarPool userAvatarPool;

	@Override
	public Handler.Role getRequiredRole() {
		return Handler.Role.None;
	}

	@GET
	@Path("")
	@Produces("text/html")
	@RolesAllowed({"admin", "account_manager"})
	public Response index(Model model) throws TigaseDBException {
		model.put("avatars", userAvatarPool.getKeys());
		String output = renderTemplate("usersAvatarPool/index.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	@GET
	@Path("/{key}")
	@RolesAllowed({"admin", "account_manager"})
	public Response avatar(@PathParam("key") String key) throws TigaseDBException {
		UserAvatarPool.Avatar avatar = userAvatarPool.getItem(key);
		if (avatar == null) {
			return Response.status(HttpStatus.NOT_FOUND_404).build();
		} else {
			return Response.ok(avatar.data(), avatar.mimeType())
					.expires(Date.from(LocalDateTime.now().plusHours(1).atZone(
							ZoneOffset.systemDefault()).toInstant()))
					.build();
		}
	}

	@POST
	@Path(value = "")
	@RolesAllowed({"admin", "account_manager"})
	public Response upload(@FormParam("file") Part filePart)
			throws IOException, NoSuchAlgorithmException, TigaseDBException {
		log.finest(() -> "uploading avatar " + filePart.getContentType() + " of " +
				filePart.getSize());
		byte[] data = filePart.getInputStream().readAllBytes();
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(data);
		String id = Algorithms.bytesToHex(md.digest());
		userAvatarPool.setItem(new UserAvatarPool.Avatar(id, filePart.getContentType(), data.length, data));
		return Response.ok().build();
	}

	@DELETE
	@Path(value = "/{key}")
	@RolesAllowed({"admin", "account_manager"})
	public Response deleteAvatar(@PathParam("key") String key) throws TigaseDBException {
		userAvatarPool.removeItem(key);
		return Response.ok().build();
	}

	@GET
	@Path("/assignRandom/{user}")
	@RolesAllowed({"admin", "account_manager"})
	public void assignRandom(@PathParam("user") BareJID userJid,  @Suspended AsyncResponse response) throws TigaseDBException {
		if (!permissionsHelper.canManageUser(userJid)) {
			response.resume(new HttpException("Forbidden", 403));
			return;
		}
		userAvatarPool.setRandomAvatarToUser(userJid).whenComplete((r,ex) -> {
		    if (ex != null) {
				response.resume(ex);
			} else {
				response.resume(Response.ok().build());
			}
		});
	}

}
