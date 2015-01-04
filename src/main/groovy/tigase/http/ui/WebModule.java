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
package tigase.http.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.http.AbstractModule;
import tigase.http.DeploymentInfo;
import tigase.http.HttpServer;
import static tigase.http.Module.HTTP_CONTEXT_PATH_KEY;
import static tigase.http.Module.HTTP_SERVER_KEY;
import static tigase.http.Module.VHOSTS_KEY;
import static tigase.http.ui.WarServlet.WAR_PATH_KEY;

/**
 *
 * @author andrzej
 */
public class WebModule extends AbstractModule {

	private static final Logger log = Logger.getLogger(WebModule.class.getCanonicalName());
	
	private HttpServer httpServer = null;
	private DeploymentInfo deployment = null;
	private String contextPath = null;
	private String[] vhosts = null;	
	private String warPath;
	
	@Override
	public String getName() {
		return "ui";
	}

	@Override
	public String getDescription() {
		return "Web UI XMPP client and management utility";
	}
	
	@Override
	public Map<String, Object> getDefaults() {
		Map<String,Object> props = super.getDefaults();
		props.put(HTTP_CONTEXT_PATH_KEY, "/" + getName());
		File[] files = new File("jars").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".war") && name.startsWith("tigase-web-ui");
			}
		});
		if (files != null && files.length > 0) {
			props.put(WAR_PATH_KEY, files[0].getAbsolutePath());
		} else {
			props.put("active", false);
		}
		return props;
	}
	
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		if (props.size() == 1)
			return;
		
		if (props.containsKey(HTTP_SERVER_KEY)) {
			httpServer = (HttpServer) props.get(HTTP_SERVER_KEY);
		}
		
		if (props.containsKey(HTTP_CONTEXT_PATH_KEY)) {
			contextPath = (String) props.get(HTTP_CONTEXT_PATH_KEY);		
		}
		
		vhosts = (String[]) props.get(VHOSTS_KEY);
		warPath = (String) props.get(WAR_PATH_KEY);
	}	
	
	@Override
	public void start() {
		if (deployment != null) {
			stop();
		}
		
		super.start();
		
		if (warPath != null) {
			deployment = HttpServer.deployment()
					.setClassLoader(this.getClass().getClassLoader())
					.setContextPath(contextPath)
					.setDeploymentName("UI")
					.addServlets(HttpServer.servlet("WarServlet", WarServlet.class).addMapping("/*")
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
