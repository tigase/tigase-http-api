/*
 * Tigase HTTP API - Jetty - Tigase HTTP API - support for Jetty HTTP Server
 * Copyright (C) 2014 Tigase, Inc. (office@tigase.com)
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

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
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
import tigase.kernel.beans.config.ConfigField;
import tigase.net.SocketType;

import javax.net.ssl.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.http.jetty.JettyHttpServerHelper.CONTEXT_KEY;
import static tigase.http.jetty.JettyHttpServerHelper.createServletContextHandler;

/**
 * This implementation embeds Jetty HTTP Server by starting separate instance which is configured and managed by
 * Tigase.
 *
 * @author andrzej
 */
@Bean(name = "httpServer", exportable = true, active = true)
public class JettyStandaloneHttpServer
		extends AbstractHttpServer
		implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(JettyStandaloneHttpServer.class.getCanonicalName());
	private final ContextHandlerCollection contexts = new ContextHandlerCollection();
	private List<DeploymentInfo> deploymentInfos = new CopyOnWriteArrayList<>();
	private Server server = new Server();

	@Override
	public List<DeploymentInfo> listDeployed() {
		return Collections.unmodifiableList(deploymentInfos);
	}

	@Override
	public void deploy(DeploymentInfo deployment) {
		ServletContextHandler context = createServletContextHandler(deployment, this);
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
	public void beforeUnregister() {
		try {
			server.stop();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception stopping internal HTTP server", ex);
		}
		super.beforeUnregister();
	}

	@Override
	public void initialize() {
		server.setHandler(contexts);
		try {
			server.start();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception starting internal HTTP server", ex);
		}
		super.initialize();;
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

	protected void registerConnector(ServerConnector connector, boolean secure) {
		server.addConnector(connector);
		if (secure) {
			httpsPorts.add(connector.getPort());
		} else {
			httpPorts.add(connector.getPort());
		}
//		if (server.isStarted() || server.isStarting()) {
//			try {
//				connector.start();
//			} catch (Exception e) {
//				log.log(Level.SEVERE, "Exception starting HTTP connector", ex);
//			}
//		}
	}

	protected void unregisterConnector(ServerConnector connector, boolean secure) {
		server.removeConnector(connector);
		if (secure) {
			httpsPorts.remove((Integer) connector.getPort());
		} else {
			httpPorts.remove((Integer) connector.getPort());
		}
	}

	protected ServerConnector createConnector(PortConfigBean config) {
		return createConnector(server, config);
	}

	protected ServerConnector createConnector(Server server, PortConfigBean config) {
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
			SslContextFactory contextFactory = new SslContextFactory.Server() {
				@Override
				public void customize(SSLEngine sslEngine) {
					super.customize(sslEngine);
					SSLParameters sslParameters = sslEngine.getSSLParameters();
					sslParameters.setSNIMatchers(Collections.singleton(new SNIMatcher(0) {
						@Override
						public boolean matches(SNIServerName sniServerName) {
							return true;
						}
					}));
					sslEngine.setSSLParameters(sslParameters);
				}
			};
			contextFactory.setSslContext(context);
			if (config.useHttp2) {
						contextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
						contextFactory.setUseCipherSuitesOrder(true);

						HttpConfiguration httpConfig = new HttpConfiguration();
						httpConfig.setSecureScheme("https");
						httpConfig.setSecurePort(config.getPort());
						httpConfig.setSendXPoweredBy(true);
						httpConfig.setSendServerVersion(true);

						HttpConnectionFactory http1 = new HttpConnectionFactory(httpConfig);
						HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(httpConfig);

						ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
						alpn.setDefaultProtocol(http1.getProtocol());
						connector = new ServerConnector(server, contextFactory, alpn, http2, http1);
			} else {
				connector = new ServerConnector(server, contextFactory);
			}
		}
		connector.setPort(config.getPort());
		return connector;
	}

	public static class PortConfigBean
			extends AbstractHttpServer.PortConfigBean {

		private static final boolean isHttp2Available() {
			return Optional.ofNullable(System.getProperty("java.version"))
					.map(version -> version.split("\\.")[0])
					.map(Integer::parseInt)
					.map(version -> version >= 9)
					.orElse(false);
		}

		private ServerConnector connector = null;
		@Inject
		private JettyStandaloneHttpServer serverManager;
		@ConfigField(desc = "Enable HTTP2 if supported", alias = "use-http2")
		private Boolean useHttp2 = isHttp2Available();

		@Override
		public void beforeUnregister() {
			if (connector == null) {
				return;
			}

			serverManager.unregisterConnector(connector, getSocket() != SocketType.plain);
			connector = null;
		}

		@Override
		public void initialize() {
			if (serverManager != null) {
				connector = serverManager.createConnector(this);
				serverManager.registerConnector(connector, getSocket() != SocketType.plain);
			}
		}

		@Override
		public void beanConfigurationChanged(Collection<String> changedFields) {
			if (getSocket() == SocketType.plain || !isHttp2Available()) {
				useHttp2 = false;
			}
			beforeUnregister();
			initialize();
		}
	}

	@Bean(name = "connections", parent = JettyStandaloneHttpServer.class, active = true, exportable = true)
	public static class PortsConfigBean
			extends AbstractHttpServer.PortsConfigBean {

		@Override
		public Class<?> getDefaultBeanClass() {
			return PortConfigBean.class;
		}
	}

}
