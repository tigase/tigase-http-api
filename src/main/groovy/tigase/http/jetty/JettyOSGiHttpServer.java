/*
 * Tigase HTTP API
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import tigase.http.Activator;
import tigase.http.DeploymentInfo;
import tigase.http.api.HttpServerIfc;
import tigase.kernel.core.Kernel;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.http.jetty.JettyHttpServerHelper.CONTEXT_KEY;
import static tigase.http.jetty.JettyHttpServerHelper.createServletContextHandler;

/**
 * This implementation uses Jetty HTTP Server instance exising in OSGi environment
 * to deploy Tigase HTTP API web apps on it.
 *
 * @author andrzej
 */
public class JettyOSGiHttpServer implements HttpServerIfc {

	private static final Logger log = Logger.getLogger(JettyOSGiHttpServer.class.getCanonicalName());
	
	private final BundleContext context;
	private final ConcurrentHashMap<String,ServiceRegistration> registeredContexts = new ConcurrentHashMap<String,ServiceRegistration>();
	private List<DeploymentInfo> deploymentInfos = new CopyOnWriteArrayList<>();

	public JettyOSGiHttpServer() {
		context = Activator.getContext();
	}
	
	protected void deploy(ServletContextHandler ctx) {
		String contextPath = ctx.getContextPath();
		
		Hashtable props = new Hashtable();
		props.put("contextFilePath", "/etc/tigase-http-context.xml");
		ServiceRegistration registration = context.registerService(ContextHandler.class.getName(), ctx, props);
		if (registration == null) {
			log.log(Level.SEVERE, "registration failed for {0}", contextPath);
		}
		registeredContexts.put(contextPath, registration);
	}

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
		}
	}

	@Override
	public List<Integer> getHTTPPorts() {
		return null;
	}

	@Override
	public List<Integer> getHTTPSPorts() {
		return null;
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}

	@Override
	public List<DeploymentInfo> listDeployed() {
		return Collections.unmodifiableList(deploymentInfos);
	}

	@Override
	public void deploy(DeploymentInfo deployment) {
		ServletContextHandler context = createServletContextHandler(deployment);
		deploy(context);
		deployment.put(CONTEXT_KEY, context);
		deploymentInfos.add(deployment);
	}

	@Override
	public void undeploy(DeploymentInfo deployment) {
		ServletContextHandler context = deployment.get(CONTEXT_KEY);
		if (context != null) {
			undeploy(context);
		}
		deploymentInfos.remove(deployment);
	}

}
