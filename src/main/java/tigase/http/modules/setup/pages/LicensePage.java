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