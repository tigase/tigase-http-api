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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import tigase.db.TigaseDBException;
import tigase.http.AuthProvider;
import tigase.http.jaxrs.Model;
import tigase.http.jaxrs.annotations.LoginForm;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Bean(name = "authentication", parent = DashboardModule.class, active = true)
public class AuthHandler extends DashboardHandler {

	@Inject
	private AuthProvider authProvider;
	@Inject(nullAllowed = true)
	private UserRegisterHandler userRegisterHandler;
	@Inject(nullAllowed = true)
	private UserPasswordRecovery userPasswordRecovery;

	@Override
	public Role getRequiredRole() {
		return Role.None;
	}

	@GET
	@Path("/login")
	@LoginForm
	@Produces(MediaType.TEXT_HTML)
	public Response loginForm(UriInfo uriInfo, Model model) {
		model.put("registrationEnabled", userRegisterHandler != null && userRegisterHandler.isRegistrationEnabled());
		model.put("passwordResetEnabled", userPasswordRecovery != null && userPasswordRecovery.isPasswordResetEnabled());
		String output = renderTemplate("login.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	@POST
	@Path("/login")
	public Response login(@FormParam("jid") @NotEmpty BareJID jid, @FormParam("password") @NotBlank String password,
						  HttpServletRequest request, HttpServletResponse response, UriInfo uriInfo, Model model)
			throws NoSuchAlgorithmException, InvalidKeyException, TigaseDBException, TigaseStringprepException {
		if (!authProvider.checkCredentials(jid.toString(), password)) {
			model.put("error", "Invalid username or password.");
			return loginForm(uriInfo, model);
		}
		authProvider.setAuthenticationCookie(response, new AuthProvider.JWTPayload(jid, request.getServerName(),
																				   LocalDateTime.now().plus(authProvider.getAuthenticationTokenValidityDuration())),
											 request.getServerName(), request.getContextPath());
		return IndexHandler.redirectToIndex(uriInfo);
	}

	@POST
	@Path("/logout")
	public Response logout(HttpServletRequest request, HttpServletResponse response, UriInfo uriInfo) {
		authProvider.resetAuthenticationCookie(response, request.getServerName(), request.getContextPath());
		return IndexHandler.redirectToIndex(uriInfo);
	}
}
