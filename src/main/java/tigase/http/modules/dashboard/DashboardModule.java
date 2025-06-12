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
package tigase.http.modules.dashboard;

import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.jaxrs.AbstractJaxRsModule;
import tigase.http.jaxrs.Handler;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.util.AssetsServlet;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Bean(name = "dashboard", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class DashboardModule extends AbstractJaxRsModule<Handler> {

	private DeploymentInfo httpDeployment;

	@ConfigField(desc = "Custom assets path", alias = "customAssetsPath")
	private String customAssetsPath;
	@ConfigField(desc = "Cache custom assets paths", alias = "customAssetsPathCached")
	private boolean customAssetsPathCached = true;

	@Override
	public String getDescription() {
		return "Dashboard of Tigase XMPP Server";
	}
	private CustomAssets customAssets;
	
	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}

		super.start();

		if (customAssetsPathCached) {
			customAssets = resolveAssets();
		}

		httpDeployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath)
				.setAuthProvider(getAuthProvider())
				.setDeploymentName("Dashboard")
				.setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}

		ServletInfo servletInfo = httpServer.servlet("JaxRsServlet", JaxRsServlet.class);
		servletInfo.addInitParam(JaxRsServlet.MODULE_KEY, uuid)
				.addMapping("/*");
		httpDeployment.addServlets(servletInfo);

		servletInfo = httpServer.servlet("AssetsServlet", AssetsServlet.class);
		servletInfo.addInitParam("customAssetsPath", customAssetsPath);
		servletInfo.addMapping("/assets/*");
		httpDeployment.addServlets(servletInfo);

		httpDeployment.setGlobalErrorPage("/error/global");

		httpServer.deploy(httpDeployment);
	}

	@Override
	public void stop() {
		if (httpDeployment != null) {
			httpServer.undeploy(httpDeployment);
			httpDeployment = null;
		}
		customAssets = null;
		super.stop();
	}

	public CustomAssets getCustomAssets() {
		if (customAssetsPath == null) {
			return CustomAssets.NONE;
		} else if (customAssets != null) {
			return customAssets;
		} else {
			return resolveAssets();
		}
	}

	private CustomAssets resolveAssets() {
		Path root = Paths.get(URI.create("file:///" + customAssetsPath));
		List<String> assets = new ArrayList<>();
		for (File file : root.toFile().listFiles()) {
			Path filePath = file.toPath();
			String assetPath = filePath.subpath(root.getNameCount(), filePath.getNameCount()).toString();
			assets.add(assetPath);
		}
		return new CustomAssets(assets.stream().filter(it -> it.endsWith(".css")).toList(),
								assets.stream().filter(it -> it.endsWith(".js")).toList());
	}

	public record CustomAssets(List<String> cssFiles, List<String> jsFiles) {
		public static CustomAssets NONE = new CustomAssets(Collections.emptyList(), Collections.emptyList());
	}

}
