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
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;

@Bean(name = "dashboard", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class DashboardModule extends AbstractJaxRsModule<Handler> {

	private DeploymentInfo httpDeployment;

	@Override
	public String getDescription() {
		return "Dashboard of Tigase XMPP Server";
	}
	
	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}

		super.start();
		
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
		servletInfo.addMapping("/assets/*");
		httpDeployment.addServlets(servletInfo);

		httpServer.deploy(httpDeployment);
	}

	@Override
	public void stop() {
		if (httpDeployment != null) {
			httpServer.undeploy(httpDeployment);
			httpDeployment = null;
		}
		super.stop();
	}

}
