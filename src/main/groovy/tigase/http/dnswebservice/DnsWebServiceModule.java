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
package tigase.http.dnswebservice;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import tigase.http.AbstractModule;
import tigase.http.HttpServer;
import static tigase.http.Module.HTTP_CONTEXT_PATH_KEY;
import static tigase.http.Module.HTTP_SERVER_KEY;
import tigase.http.rest.RestModule;
import tigase.http.security.TigasePlainLoginService;

public class DnsWebServiceModule extends AbstractModule {
	
	private ServletContextHandler httpContext;

	private HttpServer httpServer = null;
	
	private String contextPath = null;
	private String[] vhosts = null;
	
	@Override
	public String getName() {
		return "dns-webservice";
	}

	@Override
	public String getDescription() {
		return "WebService for DNS resolution";
	}

	@Override
	public Map<String, Object> getDefaults() {
		Map<String,Object> props = super.getDefaults();
		props.put(HTTP_CONTEXT_PATH_KEY, "/" + getName());
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
			
			JsonServlet jsonServlet = new JsonServlet();
			httpContext.addServlet(new ServletHolder(jsonServlet), "/*");
			
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
	
}
