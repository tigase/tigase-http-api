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

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class HttpRegistratorOSGi extends HttpRegistrator {

	private static final Logger log = Logger.getLogger(HttpRegistratorOSGi.class.getCanonicalName());
	
	private final BundleContext context;
	private final ConcurrentHashMap<String,ServiceRegistration> registeredContexts = new ConcurrentHashMap<String,ServiceRegistration>();
	
	public HttpRegistratorOSGi(BundleContext context) {
		this.context = context;
	}
	
	@Override
	public void registerContext(ServletContextHandler ctx) {
		String contextPath = ctx.getContextPath();
		
		Hashtable props = new Hashtable();
		props.put("contextFilePath", "/tigase-http-context.xml");
		ServiceRegistration registration = context.registerService(ContextHandler.class.getName(), ctx, props);
		if (registration == null) {
			log.severe("registration failed for "  + contextPath);
		}
		registeredContexts.put(contextPath, registration);
	}

	@Override
	public void unregisterContext(ServletContextHandler ctx) {
		String contextPath = ctx.getContextPath();
		try {		
			//ctx.stop();
			ServiceRegistration registration = registeredContexts.get(contextPath);
			if (registration != null) {
				registration.unregister();
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "exception during unregistration of context = " + contextPath, ctx);
		}
	}
	
}
