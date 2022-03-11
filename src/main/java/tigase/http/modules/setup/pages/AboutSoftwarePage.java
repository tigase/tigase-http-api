package tigase.http.modules.setup.pages;

import gg.jte.output.StringOutput;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tigase.http.modules.setup.InitialPage;
import tigase.http.modules.setup.NextPage;
import tigase.http.modules.setup.SetupModule;
import tigase.kernel.beans.Bean;

import javax.servlet.http.HttpServletRequest;

@Path("/about")
@InitialPage
@NextPage(LicensePage.class)
@Bean(name = "aboutSoftwarePage", parent = SetupModule.class, active = true)
public class AboutSoftwarePage extends AbstractPage {

	public String getTitle() {
		return "About software";
	}

	@GET
	public Response displayAboutSoftware() {
		StringOutput output = new StringOutput();
		engine.render("aboutSoftware.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request) {
		return redirectToNext(request);
	}

}
