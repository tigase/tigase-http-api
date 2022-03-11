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
