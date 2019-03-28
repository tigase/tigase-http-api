/**
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
package tigase.http.modules.dnswebservice;

import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.modules.AbstractModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;

@Bean(name = "dns-webservice", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class DnsWebServiceModule
		extends AbstractModule {

	private DeploymentInfo deployment = null;

	@Override
	public String getDescription() {
		return "WebService for DNS resolution";
	}

	@Override
	public void start() {
		if (deployment != null) {
			stop();
		}

		super.start();

		deployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath)
				.setDeploymentName("DnsWebService")
				.setDeploymentDescription(getDescription())
				.addServlets(httpServer.servlet("HostMeta", DnsHostMetaServlet.class).addMapping("/.well-known/*"), httpServer.servlet("JsonServlet", JsonServlet.class).addMapping("/*"));
		if (vhosts != null) {
			deployment.setVHosts(vhosts);
		}

		httpServer.deploy(deployment);
	}

	@Override
	public void stop() {
		if (deployment != null) {
			httpServer.undeploy(deployment);
			deployment = null;
		}
		super.stop();
	}

}
