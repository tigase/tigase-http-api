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
 */
package tigase.http.jetty;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import tigase.http.Activator;

/**
 *
 * @author andrzej
 */
public class JettyOSGiHttpServer extends AbstractJettyHttpServer {

	private static final Logger log = Logger.getLogger(JettyOSGiHttpServer.class.getCanonicalName());
	
	private final BundleContext context;
	private final ConcurrentHashMap<String,ServiceRegistration> registeredContexts = new ConcurrentHashMap<String,ServiceRegistration>();

	public JettyOSGiHttpServer() {
		context = Activator.getContext();
	}
	
	@Override
	protected void deploy(ServletContextHandler ctx) {
		String contextPath = ctx.getContextPath();
		
		Hashtable props = new Hashtable();
		props.put("contextFilePath", "/tigase-http-context.xml");
		ServiceRegistration registration = context.registerService(ContextHandler.class.getName(), ctx, props);
		if (registration == null) {
			log.log(Level.SEVERE, "registration failed for {0}", contextPath);
		}
		registeredContexts.put(contextPath, registration);
	}

	@Override
	protected void undeploy(ServletContextHandler ctx) {
		String contextPath = ctx.getContextPath();
		try {		
			ServiceRegistration registration = registeredContexts.get(contextPath);
			if (registration != null) {
				registration.unregister();
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "exception during unregistration of context = " + contextPath, ctx);
		}	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void setProperties(Map<String, Object> props) {
	}
	
}
