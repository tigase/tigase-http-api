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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import tigase.http.AbstractHttpServer;
import tigase.http.DeploymentInfo;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.net.SocketType;

import javax.net.ssl.SSLContext;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.http.jetty.JettyHttpServerHelper.createServletContextHandler;
import static tigase.http.jetty.JettyHttpServerHelper.CONTEXT_KEY;

/**
 * This implementation embeds Jetty HTTP Server by starting separate instance
 * which is configured and managed by Tigase.
 * 
 * @author andrzej
 */
@Bean(name = "httpServer", exportable = true)
public class JettyStandaloneHttpServer extends AbstractHttpServer implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(JettyStandaloneHttpServer.class.getCanonicalName());

	private Server server = new Server();
	private final ContextHandlerCollection contexts = new ContextHandlerCollection();

	private List<DeploymentInfo> deploymentInfos = new CopyOnWriteArrayList<>();

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


	@Override
	protected Class<?> getPortConfigBean() {
		return PortConfigBean.class;
	}

	protected void deploy(ServletContextHandler ctx) {
		contexts.addHandler(ctx);
		try {
			ctx.start();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception deploying http context " + ctx.getContextPath(), ex);
		}
	}

	protected void undeploy(ServletContextHandler ctx) {
		contexts.removeHandler(ctx);
		try {
			ctx.stop();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception undeploying http context " + ctx.getContextPath(), ex);
		}
	}

	@Override
	public void beforeUnregister() {
		try {
			server.stop();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception stopping internal HTTP server", ex);
		}
	}

	@Override
	public void initialize() {
		server.setHandler(contexts);
		try {
			server.start();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception starting internal HTTP server", ex);
		}
	}

	protected void registerConnector(ServerConnector connector) {
		server.addConnector(connector);
//		if (server.isStarted() || server.isStarting()) {
//			try {
//				connector.start();
//			} catch (Exception e) {
//				log.log(Level.SEVERE, "Exception starting HTTP connector", ex);
//			}
//		}
	}

	protected void unregisterConnector(ServerConnector connector) {
		server.removeConnector(connector);
	}

	protected ServerConnector createConnector(PortConfigBean config) {
		ServerConnector connector;
//				boolean http2Enabled = (Boolean) config.getOrDefault(HTTP2_ENABLED_KEY, true);
		if (config.getSocket() == SocketType.plain) {
//					if (http2Enabled) {
//						HttpConfiguration httpConfig = new HttpConfiguration();
//						HttpConnectionFactory http1 = new HttpConnectionFactory(httpConfig);
//						HTTP2CServerConnectionFactory http2 = new HTTP2CServerConnectionFactory(httpConfig);
//						connector = new ServerConnector(server, http1, http2);
//					} else {
			connector = new ServerConnector(server);
//					}
		} else {
			String domain = config.getDomain();
			SSLContext context = sslContextContainer.getSSLContext("TLS", domain, false);
			SslContextFactory contextFactory = new SslContextFactory();
			contextFactory.setSslContext(context);
//					if (http2Enabled) {
//						contextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
//						contextFactory.setUseCipherSuitesOrder(true);
//
//						HttpConfiguration httpConfig = new HttpConfiguration();
//						httpConfig.setSecureScheme("https");
//						httpConfig.setSecurePort(port);
//						httpConfig.setSendXPoweredBy(true);
//						httpConfig.setSendServerVersion(true);
//
//						HttpConnectionFactory http1 = new HttpConnectionFactory(httpConfig);
//						HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(httpConfig);
//
//						NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
//						ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
//						alpn.setDefaultProtocol(http1.getProtocol())
//						connector = new ServerConnector(server, contextFactory, alpn, http1, http2);
//					} else {
			connector = new ServerConnector(server, contextFactory);
//					}
		}
		connector.setPort(config.getPort());
		return connector;
	}

	public static class PortConfigBean extends AbstractHttpServer.PortConfigBean {

		@Inject
		private JettyStandaloneHttpServer serverManager;

		private ServerConnector connector = null;

		@Override
		public void beforeUnregister() {
			if (connector == null) {
				return;
			}

			serverManager.unregisterConnector(connector);
			connector = null;
		}

		@Override
		public void initialize() {
			if (getPort() != 0) {
				connector = serverManager.createConnector(this);
				serverManager.registerConnector(connector);
			}
		}

		@Override
		public void beanConfigurationChanged(Collection<String> changedFields) {
			beforeUnregister();
			initialize();
		}
	}

}
