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
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import tigase.db.AuthRepository;
import tigase.http.AbstractModule;
import tigase.http.HttpServer;
import tigase.http.security.TigasePlainLoginService;

public class RestModule extends AbstractModule {
	
	private static final String DEF_SCRIPTS_DIR_VAL = "scripts/rest";
	
	private static final String SCRIPTS_DIR_KEY = "rest-scripts-dir";

	private static final String VHOSTS_KEY = "vhosts";
	
	private static final String NAME = "rest";
	
	private ReloadHandlersCmd reloadHandlersCmd = new ReloadHandlersCmd(this);
	private String contextPath = null;
	
	private HttpServer httpServer = null;
	private ServletContextHandler httpContext = null;

	private String scriptsDir = DEF_SCRIPTS_DIR_VAL;
	private String[] vhosts = null;
	
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
		if (httpContext != null) {
			stop();
		}
		try {
			httpContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
			httpContext.setSecurityHandler(httpContext.getDefaultSecurityHandlerClass().newInstance());
			httpContext.getSecurityHandler().setLoginService(new TigasePlainLoginService());
			httpContext.setContextPath(contextPath);
			if (vhosts != null) {
				System.out.println("for module = " + getName() + " setting vhosts = " + Arrays.toString(vhosts));
				httpContext.setVirtualHosts(vhosts);
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
					startRestServletForDirectory(httpContext, dirFile);
				}
			}

			httpServer.registerContext(httpContext);
		} catch (InstantiationException ex) {
			Logger.getLogger(RestModule.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			Logger.getLogger(RestModule.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void stop() {
		if (httpContext != null) { 
			httpServer.unregisterContext(httpContext);
			httpContext = null;
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
	
    private void startRestServletForDirectory(ServletContextHandler httpContext, File scriptsDirFile) {
        File[] scriptFiles = scriptsDirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith("groovy");
            }
        });

        if (scriptFiles != null) {
            RestSerlvet restServlet = new RestSerlvet();
			restServlet.setService(new ServiceImpl(this));
            httpContext.addServlet(new ServletHolder(restServlet),"/" + scriptsDirFile.getName() + "/*");
            restServlet.loadHandlers(scriptFiles);
        }
    }	

}
