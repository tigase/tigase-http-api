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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import tigase.http.jaxrs.Model;
import tigase.kernel.beans.Bean;

@Bean(name = "index", parent = DashboardModule.class, active = true)
public class IndexHandler extends DashboardHandler {
	@Override
	public Role getRequiredRole() {
		return Role.User;
	}

	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	public Response index(UriInfo uriInfo, Model model) {
		String output = renderTemplate("index.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	public static Response redirectToIndex(UriInfo uriInfo) {
		return Response.seeOther(uriInfo.getBaseUriBuilder().path(IndexHandler.class, "index").build()).build();
	}
}
