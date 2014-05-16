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
package tigase.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import tigase.http.api.HttpServerIfc;
import tigase.http.java.JavaStandaloneHttpServer;
import tigase.http.jetty.JettyOSGiHttpServer;
import tigase.http.jetty.JettyStandaloneHttpServer;

public class HttpServer {
	
	private static final Logger log = Logger.getLogger(HttpServer.class.getCanonicalName());
	
	/**
	 * Existing HTTP server implementations:
	 * 
	 * JavaStandaloneHttpServer - internal implementation based on HttpServer from JDK
	 * JettyStandaloneHttpServer - Jetty embedded and started by Tigase in same JVM (managed by Tigase)
	 * JettyOSGiHttpServer - existing Jetty instance from OSGi environment is used by Tigase to deploy
	 */
	private static final String DEF_HTTP_SERVER_CLASS_VAL = JettyStandaloneHttpServer.class.getCanonicalName();
	private static final String HTTP_SERVER_CLASS_KEY = "server-class";

	private String serverClass = DEF_HTTP_SERVER_CLASS_VAL;
	private HttpServerIfc server = null;
	
	public Map<String,Object> getDefaults() {
		Map<String,Object> props = new HashMap<String,Object>();
		props.put(HTTP_SERVER_CLASS_KEY, DEF_HTTP_SERVER_CLASS_VAL);
		return props;
	}
	
	public void setProperties(Map<String,Object> props) {
		if (props.containsKey(HTTP_SERVER_CLASS_KEY)) {
			serverClass = (String) props.get(HTTP_SERVER_CLASS_KEY);
		}
		try {
			if (serverClass != null && (server == null || !serverClass.equals(server.getClass().getCanonicalName()))) {
				if (server != null) {
					server.stop();
				}
				server = (HttpServerIfc) this.getClass().getClassLoader().loadClass(serverClass).newInstance();
			}
			server.setProperties(props);
		} catch (Exception ex) {
			Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void start() {
		if (server != null) {
			try {
				server.start();
			} catch (Exception ex) {
				Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
			}
		}	
	}
	
	public void stop() {
		if (server != null) {
			server.stop();
		}
	}

	public void deploy(DeploymentInfo deployment) {
		server.deploy(deployment);
	}
	
	public void undeploy(DeploymentInfo deployment) {
		server.undeploy(deployment);
	}
	
	public static DeploymentInfo deployment() {
		return new DeploymentInfo();
	}
	
	public static ServletInfo servlet(String name, Class<? extends HttpServlet> servletClass) {
		return new ServletInfo(name, servletClass);
	}
}
