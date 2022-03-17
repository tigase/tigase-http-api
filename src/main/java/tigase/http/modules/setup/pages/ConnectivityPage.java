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

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

@Path("/connectivity")
@NextPage(FeaturesPage.class)
@Bean(name = "connectivityPage", parent = SetupModule.class, active = true)
public class ConnectivityPage
		extends AbstractPage {

	@Override
	public String getTitle() {
		return "Connectivity";
	}

	@GET
	public Response getForm() {
		StringOutput output = new StringOutput();
		engine.render("connectors.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request, @FormParam("c2s") boolean c2s,
								@FormParam("bosh") boolean bosh, @FormParam("ws2s") boolean ws2s,
								@FormParam("s2s") boolean s2s, @FormParam("ext") boolean ext,
								@FormParam("http") boolean http) {
		Set<String> componentsToEnable = new HashSet<>();
		if (c2s) componentsToEnable.add("c2s");
		if (bosh) componentsToEnable.add("bosh");
		if (ws2s) componentsToEnable.add("ws2s");
		if (s2s) componentsToEnable.add("s2s");
		if (ext) {
			componentsToEnable.add("ext");
			componentsToEnable.add("ext-man");
		}
		if (http) componentsToEnable.add("http");
		getConfig().setConnectors(componentsToEnable);
		return redirectToNext(request);
	}
	
}
