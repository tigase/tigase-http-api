/*
 * Tigase HTTP API
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.http.admin;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.http.AbstractModule;
import tigase.http.DeploymentInfo;
import tigase.http.HttpServer;
import static tigase.http.Module.HTTP_CONTEXT_PATH_KEY;
import static tigase.http.Module.HTTP_SERVER_KEY;
import static tigase.http.Module.VHOSTS_KEY;
import tigase.http.ServletInfo;
import tigase.http.util.StaticFileServlet;

/**
 *
 * @author andrzej
 */
public class AdminModule extends AbstractModule {

	private static final String DEF_SCRIPTS_DIR_VAL = "scripts/admin";
	
	private static final String SCRIPTS_DIR_KEY = "admin-scripts-dir";
	
	private static final String NAME =  "admin";
	private static final String DESCRIPTION = "Admin console - support for management of server using simple HTTP console";
	
	private String scriptsDir = DEF_SCRIPTS_DIR_VAL;
	private String contextPath = null;
	
	private HttpServer httpServer = null;
	private DeploymentInfo httpDeployment = null;

	private String[] vhosts = null;
		
	@Override
	public String getName() {
		return NAME;
	}

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
		httpDeployment = HttpServer.deployment().setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath).setService(new tigase.http.ServiceImpl(this));
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

		ServletInfo servletInfo = HttpServer.servlet("Servlet", Servlet.class);
		try {
			servletInfo.addInitParam(Servlet.MODULE_ID_KEY, uuid)
					.addInitParam(Servlet.SCRIPTS_DIR_KEY, scriptsDirFile.getCanonicalPath())
					.addMapping("/*");
		} catch (IOException ex) {
			Logger.getLogger(AdminModule.class.getName()).log(Level.SEVERE, null, ex);
		}
		httpDeployment.addServlets(servletInfo);
//		if (scriptDirFiles != null) {
//			for (File dirFile : scriptDirFiles) {
//				try {
//					startRestServletForDirectory(httpDeployment, dirFile);
//				} catch (IOException ex) {
//					log.log(Level.FINE, "Exception while scanning for scripts to load", ex);
//				}
//			}
//		}
		
		servletInfo = HttpServer.servlet("StaticServlet", StaticFileServlet.class);
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

	@Override
	public Map<String, Object> getDefaults() {
		Map<String,Object> props = super.getDefaults();
		props.put(HTTP_CONTEXT_PATH_KEY, "/" + getName());
		props.put(SCRIPTS_DIR_KEY, DEF_SCRIPTS_DIR_VAL);
		return props;
	}
	
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		if (props.size() == 1)
			return;
		if (props.containsKey(HTTP_CONTEXT_PATH_KEY)) {
			contextPath = (String) props.get(HTTP_CONTEXT_PATH_KEY);		
		}
		if (props.containsKey(HTTP_SERVER_KEY)) {
			httpServer = (HttpServer) props.get(HTTP_SERVER_KEY);
		}
		if (props.containsKey(SCRIPTS_DIR_KEY)) {
			scriptsDir = (String) props.get(SCRIPTS_DIR_KEY);
		}
		vhosts = (String[]) props.get(VHOSTS_KEY);
	}
	
}
