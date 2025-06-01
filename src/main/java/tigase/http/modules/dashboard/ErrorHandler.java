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
import org.eclipse.jetty.http.HttpStatus;
import tigase.http.jaxrs.Model;
import tigase.kernel.beans.Bean;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;

@Bean(name = "error", parent = DashboardModule.class, active = true)
@Path("/error")
public class ErrorHandler extends DashboardHandler {

	@Override
	public Role getRequiredRole() {
		return Role.None;
	}

	@GET
	@Path("/global")
	@Produces(MediaType.TEXT_HTML)
	public Response globalGet(HttpServletRequest request, Model model) {
		return globalError(request, model);
	}

	@POST
	@Path("/global")
	@Produces(MediaType.TEXT_HTML)
	public Response globalPost(HttpServletRequest request, Model model) {
		return globalError(request, model);
	}

	@PUT
	@Path("/global")
	@Produces(MediaType.TEXT_HTML)
	public Response globalPut(HttpServletRequest request, Model model) {
		return globalError(request, model);
	}

	@DELETE
	@Path("/global")
	@Produces(MediaType.TEXT_HTML)
	public Response globalDelete(HttpServletRequest request, Model model) {
		return globalError(request, model);
	}

	public Response globalError(HttpServletRequest request, Model model) {
		request.getAttributeNames().asIterator().forEachRemaining(attName -> {
			System.out.println(attName + " = " + request.getAttribute(attName));
		});
		int code = (int) request.getAttribute("javax.servlet.error.status_code");
		model.put("code", code);
		model.put("message", HttpStatus.getMessage(code));
		model.put("reason", request.getAttribute("javax.servlet.error.message"));
		Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
		if (throwable != null) {
			StringWriter writer = new StringWriter();
			throwable.printStackTrace(new PrintWriter(writer));
			model.put("exception", throwable);
			model.put("stacktrace", writer.toString());
		}
		String output = renderTemplate("error.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}
}
