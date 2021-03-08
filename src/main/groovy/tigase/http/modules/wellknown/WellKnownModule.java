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
package tigase.http.modules.wellknown;

import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.modules.AbstractModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;

import java.util.concurrent.CopyOnWriteArraySet;

@Bean(name = "well-known", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class WellKnownModule extends AbstractModule {

	private DeploymentInfo deployment = null;

	@Inject
	private CopyOnWriteArraySet<WellKnownServletsProvider> wellKnownServletsProviders;

	@Override
	public String getDescription() {
		return "Support for /.well-known/";
	}
	
	@Override
	public void start() {
		if (deployment != null) {
			stop();
		}
		
		deployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/.well-known")
				.setDeploymentName("Well-Known")
				.setDeploymentDescription(getDescription())
				.addServlets(wellKnownServletsProviders.stream()
									 .flatMap(provider -> provider.getServletInfos().stream())
									 .toArray(ServletInfo[]::new));
		super.start();
		
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
