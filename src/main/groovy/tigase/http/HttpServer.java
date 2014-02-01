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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class HttpServer {
	
	private static final Logger log = Logger.getLogger(HttpServer.class.getCanonicalName());
	
	private HttpRegistrator httpRegistrator;
	private static HttpRegistrator osgiHttpRegistrator;
	
	protected static void setOsgiHttpRegistrator(HttpRegistrator httpRegistrator) {
		HttpServer.osgiHttpRegistrator = httpRegistrator;
	}
	
	private static final String HTTP_PORT_KEY = "port";
	private static final String USE_LOCAL_SERVER_KEY = "use-local-server";
	
	private static final int DEF_HTTP_PORT_VAL = 8080;
	
	private int port = DEF_HTTP_PORT_VAL;
	private Server server = null;
	private boolean useLocal = true;
	
	public Map<String,Object> getDefaults() {
		Map<String,Object> props = new HashMap<String,Object>();
		props.put(HTTP_PORT_KEY, DEF_HTTP_PORT_VAL);
		props.put(USE_LOCAL_SERVER_KEY, osgiHttpRegistrator == null);
		return props;
	}
	
	public void setProperties(Map<String,Object> props) {
		if (props.containsKey(HTTP_PORT_KEY)) {
			port = (Integer) props.get(HTTP_PORT_KEY);
		}
		if (props.containsKey(USE_LOCAL_SERVER_KEY)) {
			useLocal = (Boolean) props.get(USE_LOCAL_SERVER_KEY);
		}
	}
	
	public void start() {
		if (useLocal) {
			if (httpRegistrator == null) {
				server = new Server(port);
				httpRegistrator = new HttpRegistratorInt(server);
			}
		}
		else {
			if (httpRegistrator != null && server != null) {
				stop();
			}
			httpRegistrator = osgiHttpRegistrator;
		}
	}
	
	public void stop() {
		if (server != null) {
			httpRegistrator = null;
			try {
				server.stop();
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Exception stopping internal HTTP server", ex);
			}
		}
	}
	
	public void registerContext(ServletContextHandler context) {
		if (log.isLoggable(Level.INFO)) {
			String[] vhosts = context.getVirtualHosts();
			log.log(Level.INFO, "registering context for = {0} for virtual hosts = {1} using {2}", 
					new Object[]{context.getContextPath(), Arrays.toString(vhosts),
						httpRegistrator.getClass().getCanonicalName()});
		}
		httpRegistrator.registerContext(context);
	}
	
	public void unregisterContext(ServletContextHandler context) {
		if (log.isLoggable(Level.INFO)) {
			String[] vhosts = context.getVirtualHosts();
			log.log(Level.INFO, "unregistering context for = {0} for virtual hosts = {1} using {2}", 
					new Object[]{context.getContextPath(), Arrays.toString(vhosts), 
						httpRegistrator.getClass().getCanonicalName()});
		}
		httpRegistrator.unregisterContext(context);
	}
	
}
