/*
 * Tigase HTTP API
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.rest;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.http.AbstractModule;
import tigase.http.DeploymentInfo;
import tigase.http.HttpServer;

import tigase.http.ServletInfo;

public class RestModule extends AbstractModule {
	
	private static final Logger log = Logger.getLogger(RestModule.class.getCanonicalName());
	
	private static final String DEF_SCRIPTS_DIR_VAL = "scripts/rest";
	
	private static final String SCRIPTS_DIR_KEY = "rest-scripts-dir";

	private static final String NAME = "rest";
	
	private final ReloadHandlersCmd reloadHandlersCmd = new ReloadHandlersCmd(this);
	private String contextPath = null;
	
	private HttpServer httpServer = null;
	private DeploymentInfo httpDeployment = null;

	private String scriptsDir = DEF_SCRIPTS_DIR_VAL;
	private String[] vhosts = null;
	
	private final String uuid = UUID.randomUUID().toString();
	
	private static final ConcurrentHashMap<String,RestModule> modules = new ConcurrentHashMap<String,RestModule>();
	
	public static RestModule getModuleByUUID(String uuid) {
		return modules.get(uuid);
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String getDescription() {
		return "REST support - handles HTTP REST access using scripts";
	}
	
	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}
		modules.put(uuid, this);

		httpDeployment = HttpServer.deployment().setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath).setService(new ServiceImpl(this));
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}
		File scriptsDirFile = new File(scriptsDir);
		File[] scriptDirFiles = scriptsDirFile.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory();
			}
		});

		if (scriptDirFiles != null) {
			for (File dirFile : scriptDirFiles) {
				try {
					startRestServletForDirectory(httpDeployment, dirFile);
				} catch (IOException ex) {
					log.log(Level.FINE, "Exception while scanning for scripts to load", ex);
				}
			}
		}
		httpServer.deploy(httpDeployment);
	}

	@Override
	public void stop() {
		if (httpDeployment != null) { 
			httpServer.undeploy(httpDeployment);
			httpDeployment = null;
			modules.remove(uuid, this);
		}
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
		
		commandManager.registerCmd(reloadHandlersCmd);
	}
	
    private void startRestServletForDirectory(DeploymentInfo httpDeployment, File scriptsDirFile) 
			throws IOException {
        File[] scriptFiles = getGroovyFiles(scriptsDirFile);

        if (scriptFiles != null) {
			ServletInfo servletInfo = HttpServer.servlet("RestServlet", RestExtServlet.class);
			servletInfo.addInitParam(RestServlet.REST_MODULE_KEY, uuid)
					.addInitParam(RestServlet.SCRIPTS_DIR_KEY, scriptsDirFile.getCanonicalPath())
					.addMapping("/" + scriptsDirFile.getName() + "/*");
			httpDeployment.addServlets(servletInfo);
        }
    }	

	public static File[] getGroovyFiles( File scriptsDirFile) {
		return scriptsDirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith("groovy");
            }
        });
	}
}
