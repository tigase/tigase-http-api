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

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import gg.jte.resolve.ResourceCodeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tigase.http.api.HttpException;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.xmpp.impl.JabberIqRegister;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Path("/user")
@Bean(name = "userConfirmAccount", parent = RestModule.class, active = true)
public class UserConfirmAccountHandler
		extends AbstractRestHandler {

	@Inject(nullAllowed = true)
	private JabberIqRegister.AccountValidator[] validators;

	private final TemplateEngine templateEngine;

	public UserConfirmAccountHandler() {
		super(Security.None, Role.None);
		templateEngine = TemplateEngine.create(new ResourceCodeResolver("tigase/rest"), ContentType.Html);
	}

	@GET
	@Path("/{token}/confirm")
	@Produces({"text/html"})
	@Operation(summary = "Confirm account", description = "Returns website with result of account confirmation")
	@ApiResponse(responseCode = "200", description = "HTML website")
	@ApiResponse(responseCode = "417", description = "Account confirmation is not enabled on the server")
	public Response confirm(@Parameter(description = "Token to confirm account") @PathParam("token") String token, HttpServletRequest request)
			throws HttpException {
		if (validators == null) {
			throw new HttpException("Account validators are not configured!", HttpServletResponse.SC_EXPECTATION_FAILED);
		}
		Map<String, Object> context = new HashMap<>();

		for (JabberIqRegister.AccountValidator validator : validators) {
			try {
				context.put("account", validator.validateAccount(token));
			} catch (RuntimeException ex) {
				context.put("exception", ex);
			}
		}

		StringOutput output = new StringOutput();
		context.put("basePath", request.getContextPath());
		templateEngine.render("confirmAccount.jte", context, output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

}
