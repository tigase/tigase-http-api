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
package tigase.http.modules.admin;

import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.modules.AbstractModule;
import tigase.http.util.StaticFileServlet;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author andrzej
 */
@Bean(name = "admin", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class AdminModule
		extends AbstractModule {

	private static final String DEF_SCRIPTS_DIR_VAL = "scripts/admin";

	private static final String SCRIPTS_DIR_KEY = "admin-scripts-dir";

	private static final String DESCRIPTION = "Admin console - support for management of server using simple HTTP console";
	private DeploymentInfo httpDeployment = null;
	@ConfigField(desc = "Scripts directory", alias = SCRIPTS_DIR_KEY)
	private String scriptsDir = DEF_SCRIPTS_DIR_VAL;

	@Override
	public String getDescription() {
		return DESCRIPTION;
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
				.setService(new tigase.http.ServiceImpl(this))
				.setDeploymentName("Admin console")
				.setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}
		File scriptsDirFile = new File(scriptsDir);
		File[] scriptDirFiles = scriptsDirFile.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory() && !"static".equals(file.getName());
			}
		});

		ServletInfo servletInfo = httpServer.servlet("Servlet", Servlet.class);
		try {
			servletInfo.addInitParam(Servlet.MODULE_ID_KEY, uuid)
					.addInitParam(Servlet.SCRIPTS_DIR_KEY, scriptsDirFile.getCanonicalPath())
					.addMapping("/*");
		} catch (IOException ex) {
			Logger.getLogger(AdminModule.class.getName()).log(Level.WARNING, null, ex);
		}
		httpDeployment.addServlets(servletInfo);

		servletInfo = httpServer.servlet("StaticServlet", StaticFileServlet.class);
		servletInfo.addInitParam(StaticFileServlet.DIRECTORY_KEY, new File(scriptsDirFile, "static").getAbsolutePath())
				.addMapping("/static/*");
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
