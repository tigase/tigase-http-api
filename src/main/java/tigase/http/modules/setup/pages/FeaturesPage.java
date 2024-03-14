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