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

@Path("/database")
@NextPage(DatabasePreparationPage.class)
@Bean(name = "databaseConfigPage", parent = SetupModule.class, active = true)
public class DatabaseConfigPage extends AbstractPage {

	@Override
	public String getTitle() {
		return "Database configuration";
	}

	@GET
	public Response getConfigForm() {
		StringOutput output = new StringOutput();
		engine.render("dbConfig.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request, @BeanParam Config.DBConfig dbConfig) {
		getConfig().setDbConfig(dbConfig);
		return redirectToNext(request);
	}

}
