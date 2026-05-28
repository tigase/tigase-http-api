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
package tigase.http.modules.setup;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import tigase.db.TigaseDBException;
import tigase.http.AuthProvider;
import tigase.http.jaxrs.Handler;
import tigase.http.jaxrs.Model;
import tigase.http.jaxrs.annotations.LoginForm;
import tigase.http.modules.dashboard.IndexHandler;
import tigase.http.util.TemplateUtils;
import tigase.kernel.beans.Bean;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;

@Bean(name = "login", parent = SetupModule.class, active = true)
public class LoginPage implements Handler, SetupHandler {

	protected final TemplateEngine engine;
	
	private AuthProvider authProvider;

	public LoginPage() {
		this.engine = TemplateUtils.create(null, "tigase.setup", ContentType.Html);
	}

	public void setAuthProvider(AuthProvider authProvider) {
		this.authProvider = authProvider;
	}

	@Override
	public String getPath() {
		return null;
	}

	@Override
	public String getTitle() {
		return "Login";
	}

	@Override
	public Role getRequiredRole() {
		return Role.None;
	}

	@GET
	@Path("/login")
	@LoginForm
	@Produces(MediaType.TEXT_HTML)
	public Response loginForm(UriInfo uriInfo, Model model) {
		StringOutput output = new StringOutput();
		engine.render("login.jte", new HashMap<>(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	@Path("/login")
	public Response login(@FormParam("username") @NotEmpty BareJID username, @FormParam("password") @NotBlank String password,
	                      HttpServletRequest request, HttpServletResponse response, UriInfo uriInfo, Model model)
			throws NoSuchAlgorithmException, InvalidKeyException, TigaseDBException, TigaseStringprepException {
		if (!authProvider.checkCredentials(username.toString(), password)) {
			model.put("error", "Invalid username or password.");
			return loginForm(uriInfo, model);
		}
		authProvider.setAuthenticationCookie(response, new AuthProvider.JWTPayload(username, request.getServerName(),
		                                                                           LocalDateTime.now().plus(authProvider.getAuthenticationTokenValidityDuration())),
		                                     request.getServerName(), request.getContextPath());
		return Response.temporaryRedirect(uriInfo.getBaseUriBuilder().fragment("/").build()).build();
	}

	@POST
	@Path("/logout")
	public Response logout(HttpServletRequest request, HttpServletResponse response, UriInfo uriInfo) {
		authProvider.resetAuthenticationCookie(response, request.getServerName(), request.getContextPath());
		return IndexHandler.redirectToIndex(uriInfo);
	}
}
