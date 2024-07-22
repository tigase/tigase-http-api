package tigase.http.modules.setup.pages;

import gg.jte.output.StringOutput;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.NextPage;
import tigase.http.modules.setup.SetupModule;
import tigase.kernel.beans.Bean;

import javax.servlet.http.HttpServletRequest;

@Path("/voip")
@NextPage(FeaturesPage.class)
@Bean(name = "voipPage", parent = SetupModule.class, active = true)
public class VoipPage
		extends AbstractPage {

	@Override
	public String getTitle() {
		return "VoIP configuration";
	}

	@GET
	public Response getForm() {
		StringOutput output = new StringOutput();
		engine.render("voip.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request, @BeanParam Config.VoipConfig voipConfig) {
		getConfig().setVoipConfig(voipConfig);
		return redirectToNext(request);
	}

}