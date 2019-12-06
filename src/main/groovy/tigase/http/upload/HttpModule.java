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
package tigase.http.upload;

import tigase.http.AbstractHttpModule;
import tigase.http.DeploymentInfo;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;

/**
 * Created by andrzej on 08.08.2016.
 */
@Bean(name = "httpModule", parent = FileUploadComponent.class, active = true)
public class HttpModule
		extends AbstractHttpModule
		implements Initializable, UnregisterAware {

	@Inject
	private FileServlet.FileServletContext context;

	private DeploymentInfo deployment;

	public HttpModule() {
		contextPath = "/upload";
	}

	@Override
	public void start() {
		if (deployment != null) {
			stop();
		}

		deployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath)
				.setVHosts(vhosts)
				.setDeploymentName("httpFileUpload")
				.setDeploymentDescription("XEP-0363: HTTP File Upload")
				.addServlets(httpServer.servlet("FileServlet", FileServlet.class)
									 .addMapping("/*")
									 .addInitParam("kernel", uuid));

		httpServer.deploy(deployment);
	}

	@Override
	public void stop() {
		httpServer.undeploy(deployment);
		deployment = null;
	}

}
