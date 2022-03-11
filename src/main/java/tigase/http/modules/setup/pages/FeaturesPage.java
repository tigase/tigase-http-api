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

@Path("/features")
@NextPage(DatabaseConfigPage.class)
@Bean(name = "featuresPage", parent = SetupModule.class, active = true)
public class FeaturesPage extends AbstractPage {

	@Override
	public String getTitle() {
		return "Features";
	}

	@GET
	public Response getForm() {
		StringOutput output = new StringOutput();
		engine.render("features.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request, @FormParam("clusterMode") boolean clusterMode,
								@FormParam("acs") boolean acs, @FormParam("muc") boolean muc,
								@FormParam("pubsub") boolean pubsub, @FormParam("mix") boolean mix,
								@FormParam("mam") boolean mam, @FormParam("push") boolean push,
								@FormParam("upload") boolean upload, @FormParam("carbons") boolean carbons,
								@FormParam("csi") boolean csi, @FormParam("motd") boolean motd,
								@FormParam("lastActivity") boolean lastActivity, @FormParam("spam") boolean spam) {
		getConfig().setClusterMode(clusterMode);
		getConfig().setACSEnabled(acs);
		Set<String> features = new HashSet<>();
		if (muc) features.add("muc");
		if (pubsub) features.add("pubsub");
		if (mix) features.add("mix");
		if (mam) features.add("mam");
		if (push) features.add("push");
		if (upload) features.add("upload");
		if (carbons) features.add("carbons");
		if (csi) features.add("csi");
		if (motd) features.add("motd");
		if (lastActivity) features.add("lastActivity");
		if (spam) features.add("spam");
		
		getConfig().setFeatures(features);
		return redirectToNext(request);
	}
}
