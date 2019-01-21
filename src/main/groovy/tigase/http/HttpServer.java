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
package tigase.http;

import tigase.conf.ConfigurationException;
import tigase.http.api.HttpServerIfc;
import tigase.http.java.JavaStandaloneHttpServer;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.XMPPServer;

import javax.servlet.http.HttpServlet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServer {
	
	private static final Logger log = Logger.getLogger(HttpServer.class.getCanonicalName());

	/**
	 * Existing HTTP server implementations:
	 * 
	 * JavaStandaloneHttpServer - internal implementation based on HttpServer from JDK
	 * JettyStandaloneHttpServer - Jetty embedded and started by Tigase in same JVM (managed by Tigase)
	 * JettyOSGiHttpServer - existing Jetty instance from OSGi environment is used by Tigase to deploy
	 */
	private static final String DEF_HTTP_SERVER_CLASS_VAL = JavaStandaloneHttpServer.class.getCanonicalName();
	private static final String HTTP_SERVER_CLASS_KEY = "server-class";

	private CopyOnWriteArrayList<DeploymentInfo> deployed = new CopyOnWriteArrayList<>();
	private String serverClass = DEF_HTTP_SERVER_CLASS_VAL;
	private HttpServerIfc server = null;
	
	public Map<String,Object> getDefaults() {
		Map<String,Object> props = new HashMap<String,Object>();
		props.put(HTTP_SERVER_CLASS_KEY, DEF_HTTP_SERVER_CLASS_VAL);
		return props;
	}
	
	public void setProperties(Map<String,Object> props) throws ConfigurationException {
		if (props.containsKey(HTTP_SERVER_CLASS_KEY)) {
			serverClass = (String) props.get(HTTP_SERVER_CLASS_KEY);
		}
		try {
			if (serverClass != null && (server == null || !serverClass.equals(server.getClass().getCanonicalName()))) {
				if (server != null) {
					server.stop();
				}
				Class<?> cls = ModulesManagerImpl.getInstance().forName(serverClass);
				server = (HttpServerIfc) cls.newInstance();
			}
			server.setProperties(props);
		} catch (Exception e) {
				if (!XMPPServer.isOSGi()) {
					log.log(Level.SEVERE, "Cannot instantiate HTTP server implementation class: " +
							serverClass, e);
				}
				throw new ConfigurationException("Can not instantiate HTTP server implementation class: " +
					serverClass);	
		}
	}
	
	public void start() {
		if (server != null) {
			try {
				server.start();
			} catch (Exception ex) {
				Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, "start of HTTP server failed", ex);
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
		deployed.add(deployment);
	}
	
	public void undeploy(DeploymentInfo deployment) {
		deployed.remove(deployment);
		server.undeploy(deployment);
	}

	public List<DeploymentInfo> listDeployed() {
		return deployed;
	}

	public static DeploymentInfo deployment() {
		return new DeploymentInfo();
	}
	
	public static ServletInfo servlet(String name, Class<? extends HttpServlet> servletClass) {
		return new ServletInfo(name, servletClass);
	}
}
