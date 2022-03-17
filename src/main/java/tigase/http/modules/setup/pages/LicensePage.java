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
import tigase.http.api.HttpException;
import tigase.http.api.NotFoundException;
import tigase.http.modules.setup.NextPage;
import tigase.http.modules.setup.SetupModule;
import tigase.kernel.beans.Bean;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Path("/license")
@Bean(name = "licensePage", parent = SetupModule.class, active = true)
@NextPage(BasicConfigPage.class)
public class LicensePage
		extends AbstractPage {

	public String getTitle() {
		return "License";
	}

	@GET
	public Response displayLicenseInfo() {
		StringOutput output = new StringOutput();
		engine.render("license.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@GET
	@Path("/agpl.html")
	public Response displayAGPL() throws HttpException, IOException {
		File licenceFile = new File("License.html");
		if (!licenceFile.exists()) {
			throw new NotFoundException("File is missing");
		}


		try (FileInputStream fis = new FileInputStream(licenceFile)) {
			return Response.ok(fis.readAllBytes(), MediaType.TEXT_HTML).build();
		}
	}

	@POST
	public Response processForm(HttpServletRequest request, @FormParam("companyName") String companyName) {
		if (getConfig().installationContainsACS()) {
			Optional<String> value = Optional.ofNullable(companyName)
					.map(String::trim)
					.filter(str -> !str.isEmpty());
			if (value.isPresent()) {
				getConfig().setCompanyName(value.get());
			} else {
				return Response.seeOther(URI.create(request.getRequestURI())).build();
			}
		}
		return redirectToNext(request);
	}
	
}