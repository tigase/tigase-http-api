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
package tigase.http.modules.setup.pages;

import gg.jte.output.StringOutput;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tigase.http.modules.setup.NextPage;
import tigase.http.modules.setup.SetupModule;
import tigase.kernel.beans.Bean;

import jakarta.servlet.http.HttpServletRequest;

@Path("/security")
@NextPage(SaveConfigPage.class)
@Bean(name = "securityPage", parent = SetupModule.class, active = true)
public class SetupSecurityPage extends AbstractPage{

	@Override
	public String getTitle() {
		return "Setup security";
	}

	@GET
	public Response getConfigForm() {
		StringOutput output = new StringOutput();
		engine.render("security.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request, @FormParam("username") String setupUser, @FormParam("password") String setupPassword) {
		getConfig().setSetupUser(setupUser);
		getConfig().setSetupPassword(setupPassword);
		return redirectToNext(request);
	}
}