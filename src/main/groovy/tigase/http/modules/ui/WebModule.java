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
package tigase.http.modules.ui;

import tigase.http.DeploymentInfo;
import tigase.http.modules.AbstractModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;

import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.http.modules.ui.WarServlet.WAR_PATH_KEY;

/**
 * @author andrzej
 */
@Bean(name = "webModule", active = true)
public class WebModule
		extends AbstractModule {

	private static final Logger log = Logger.getLogger(WebModule.class.getCanonicalName());
	@ConfigField(desc = "Path to WAR file", alias = WAR_PATH_KEY)
	protected String warPath;
	private DeploymentInfo deployment = null;
	@ConfigField(desc = "Description of a module")
	protected String description = "Simple static WAR deployment module";

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void start() {
		if (deployment != null) {
			stop();
		}

		super.start();

		if (warPath != null) {
			deployment = httpServer.deployment()
					.setClassLoader(this.getClass().getClassLoader())
					.setContextPath(contextPath)
					.setDeploymentName("User interface")
					.setDeploymentDescription(getDescription())
					.addServlets(httpServer.servlet("WarServlet", WarServlet.class)
										 .addMapping("/*")
										 .addInitParam(WAR_PATH_KEY, warPath));
			if (vhosts != null) {
				deployment.setVHosts(vhosts);
			}

			httpServer.deploy(deployment);
		} else {
			log.log(Level.INFO, "not found file with Web UI - Web UI will not be available");
		}
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
