/*
 * JettyHttpServerHelper.java
 *
 * Tigase HTTP API - Jetty support
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.jetty;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import tigase.http.DeploymentInfo;
import tigase.http.ServletInfo;
import tigase.http.api.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 06.08.2016.
 */
public class JettyHttpServerHelper {

	public static final String CONTEXT_KEY = "context-key";

	private static final Logger log = Logger.getLogger(JettyHttpServerHelper.class.getCanonicalName());

	public static ServletContextHandler createServletContextHandler(DeploymentInfo deployment) {

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		try {
			context.setSecurityHandler(context.getDefaultSecurityHandlerClass().newInstance());
		} catch (InstantiationException ex) {
			log.log(Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			log.log(Level.SEVERE, null, ex);
		}
		Service service = deployment.getService();
		if (service != null) {
			context.getSecurityHandler().setLoginService(new tigase.http.jetty.security.TigasePlainLoginService(service));
		}
		context.setContextPath(deployment.getContextPath());
		if (deployment.getClassLoader() != null)
			context.setClassLoader(deployment.getClassLoader());
		String[] vhosts = deployment.getVHosts();
		if (vhosts != null && vhosts.length > 0) {
			context.setVirtualHosts(vhosts);
		}
		ServletInfo[] servletInfos = deployment.getServlets();
		for (ServletInfo info : servletInfos) {
			for (String mapping : info.getMappings()) {
				ServletHolder holder = new ServletHolder(mapping, info.getServletClass());
				holder.setInitParameters(info.getInitParams());
				context.addServlet(holder, mapping);
			}
		}

		return context;
	}

}
