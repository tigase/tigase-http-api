/*
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
package tigase.http.java;

import com.sun.net.httpserver.*;
import tigase.cluster.ClusterConnectionManager;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.http.AbstractHttpServer;
import tigase.http.DeploymentInfo;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.Kernel;
import tigase.net.SocketType;

import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic implementation of HTTP server based on HttpServer implementation embedded in JDK.
 * <p>
 * May not fully support HTTP implementation but is sufficient for testing and basic usage.
 *
 * @author andrzej
 */
@Bean(name = "httpServer", parent = Kernel.class, active = true, exportable = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SetupMode})
public class JavaStandaloneHttpServer
		extends AbstractHttpServer {

	private static final Logger log = Logger.getLogger(JavaStandaloneHttpServer.class.getCanonicalName());

	private final Set<HttpServer> servers = new HashSet<HttpServer>();

	private List<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();

	@Inject
	private ExecutorWithTimeout executor;
	private boolean delayStartup = false;

	@Override
	public void deploy(DeploymentInfo deployment) {
		synchronized (servers) {
			deployments.add(deployment);
			servers.forEach(server -> this.deploy(server, deployment));
		}
	}

	@Override
	public void undeploy(DeploymentInfo deployment) {
		synchronized (servers) {
			servers.forEach(server -> this.undeploy(server, deployment));
			deployments.remove(deployment);
		}
	}

	@Override
	public List<DeploymentInfo> listDeployed() {
		return Collections.unmodifiableList(deployments);
	}

	@Override
	public void register(Kernel kernel) {
		delayStartup = ServerBeanSelector.getClusterMode(kernel) &&
				ServerBeanSelector.getConfigType(kernel) != ConfigTypeEnum.SetupMode;
		super.register(kernel);
	}

	protected HttpServer createServer(PortConfigBean config) throws IOException {
		if (config.getSocket() == SocketType.plain) {
			return HttpServer.create(new InetSocketAddress(config.getPort()), 100);
		} else {
			HttpsServer server = HttpsServer.create(new InetSocketAddress(config.getPort()), 100);
			SSLContext sslContext = sslContextContainer.getSSLContext("TLS", config.getDomain(), false);
			server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
				@Override
				public void configure(HttpsParameters httpsParameters) {
					super.configure(httpsParameters);
					SSLParameters sslParameters = this.getSSLContext().getDefaultSSLParameters();
					sslParameters.setSNIMatchers(Collections.singleton(new SNIMatcher(0) {
						@Override
						public boolean matches(SNIServerName sniServerName) {
							return true;
						}
					}));
					httpsParameters.setSSLParameters(sslParameters);
				}
			});
			return server;
		}
	}

	protected void deploy(HttpServer server) {
		Collections.unmodifiableList(deployments).forEach(info -> deploy(server, info));
	}

	protected void deploy(HttpServer server, DeploymentInfo info) {
		try {
			server.createContext(info.getContextPath(), new RequestHandler(this, info, executor.timer));
		} catch (ServletException ex) {
			String message = "Could not deploy " + info.getContextPath() + " at " + getName() + " at port " +
					server.getAddress().getPort();
			log.log(Level.WARNING, message);
			throw new RuntimeException(ex);
		}
	}

	protected void undeploy(HttpServer server) {
		Collections.unmodifiableList(deployments).forEach(info -> undeploy(server, info));
	}

	protected void undeploy(HttpServer server, DeploymentInfo info) {
		try {
			server.removeContext(info.getContextPath());
		} catch (IllegalArgumentException ex) {
			log.log(Level.FINEST, "deployment context " + info.getContextPath() + " already removed");
		}
	}

	private void registerServer(HttpServer server) {
		synchronized (servers) {
			servers.add(server);
			int port = server.getAddress().getPort();
			if (server instanceof HttpsServer) {
				httpsPorts.add(port);
			} else {
				httpPorts.add(port);
			}
			deploy(server);
		}
	}

	private void unregisterServer(HttpServer server) {
		synchronized (servers) {
			undeploy(server);
			int port = server.getAddress().getPort();
			if (server instanceof HttpsServer) {
				httpsPorts.remove((Integer) port);
			} else {
				httpPorts.remove((Integer) port);
			}
			server.getAddress().getPort();
			servers.remove(server);
		}
	}

	@Bean(name = "executor", parent = JavaStandaloneHttpServer.class, active = true, exportable = true)
	public static class ExecutorWithTimeout
			implements Executor, Initializable, UnregisterAware, ConfigurationChangedAware {

		private static final String THREADS_KEY = "threads";
		private static final String REQUEST_TIMEOUT_KEY = "request-timeout";
		private AtomicInteger counter = new AtomicInteger(0);
		private ExecutorService executor = null;
		@ConfigField(desc = "Number of threads", alias = THREADS_KEY)
		private int threads = 4;
		@ConfigField(desc = "Request timeout", alias = REQUEST_TIMEOUT_KEY)
		private int timeout = 60 * 1000;
		@ConfigField(desc = "Accept timeout", alias = "accept-timeout")
		private int acceptTimeout = 2000;
		private Timer timer;

		public ExecutorWithTimeout() {
		}

		@Override
		public void execute(final Runnable command) {
			executor.execute(() -> {
				RequestHandler.setRequestId();
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "executing for:" + RequestHandler.getRequestId() + " - " + command.toString());
				}
				timer.connectionAccepted();
				command.run();
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "execution completed for:" + RequestHandler.getRequestId() + " - " + command.toString());
				}
				timer.requestProcessingFinished();
			});
		}

		@Override
		public void beforeUnregister() {
			executor.shutdown();
			timer.shutdown();
		}

		@Override
		public void initialize() {
			if (executor != null) {
				beforeUnregister();
			}
			executor = Executors.newFixedThreadPool(threads, r -> {
				Thread t = new Thread(r);
				t.setName("http-server-pool-" + counter.incrementAndGet());
				t.setDaemon(true);
				return t;
			});
			timer = new Timer(this::getAcceptTimeout, this::getTimeout);
		}

		@Override
		public void beanConfigurationChanged(Collection<String> changedFields) {
			initialize();
		}

		public int getAcceptTimeout() {
			return acceptTimeout;
		}

		public int getTimeout() {
			return timeout;
		}

		public static class Timer {

			private ThreadLocal<ScheduledFuture> threadTimeouts = new ThreadLocal<>();

			private ScheduledExecutorService executor;
			private final Supplier<Integer> acceptTimeoutSupplier;
			public final Supplier<Integer> requestTimeoutSupplier;

			private Timer(Supplier<Integer> acceptTimeoutSupplier, Supplier<Integer> requestTimeoutSupplier) {
				executor = Executors.newSingleThreadScheduledExecutor();
				this.acceptTimeoutSupplier = acceptTimeoutSupplier;
				this.requestTimeoutSupplier = requestTimeoutSupplier;
			}

			private void shutdown() {
				executor.shutdown();;
			}

			public void connectionAccepted() {
				final Thread currentThread = Thread.currentThread();
				final int reqId = RequestHandler.getRequestId();
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "request accept timer started: " + reqId);
				}
				threadTimeouts.set(executor.schedule(() -> {
					log.log(Level.WARNING, "request accept time exceeded!" + " for id = " + reqId);
					currentThread.interrupt();
				}, acceptTimeoutSupplier.get(), TimeUnit.MILLISECONDS));
			}

			public void requestProcessingStarted() {
				ScheduledFuture timeout = threadTimeouts.get();
				if (timeout != null) {
					final int reqId = RequestHandler.getRequestId();
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "request accept timer ended: " + reqId);
					}
					timeout.cancel(false);
				}

				final Thread currentThread = Thread.currentThread();
				final int reqId = RequestHandler.getRequestId();
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "request processing timer started: " + reqId);
				}
				threadTimeouts.set(executor.schedule(() -> {
					log.log(Level.WARNING, "request processing time exceeded!" + " for id = " + reqId);
					currentThread.interrupt();
				}, requestTimeoutSupplier.get(), TimeUnit.MILLISECONDS));
			}

			public void requestProcessingFinished() {
				ScheduledFuture timeout = threadTimeouts.get();
				if (timeout != null) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "request accept/processing timer ended: " + RequestHandler.getRequestId());
					}
					timeout.cancel(false);
				}
			}

			public ScheduledExecutorService getScheduledExecutorService() {
				return executor;
			}

		}
	}

	public static class PortConfigBean
			extends AbstractHttpServer.PortConfigBean {

		protected HttpServer httpServer;
		private final EventBus eventBus = EventBusFactory.getInstance();
		@Inject(bean = "executor")
		private ExecutorWithTimeout executor;
		@Inject
		private JavaStandaloneHttpServer serverManager;
		private ScheduledFuture startupFuture;
		
		public void beanConfigurationChanged(Collection<String> changedFields) {
			if (serverManager == null) {
				return;
			}

			beforeUnregister();
			initialize();
		}

		@Override
		public void beforeUnregister() {
			startupFuture.cancel(true);
			eventBus.unregisterAll(this);
			if (httpServer != null) {
				serverManager.unregisterServer(httpServer);
				httpServer.stop(1);
				httpServer = null;
			}
		}

		@Override
		public void initialize() {
			eventBus.registerAll(this);
			startServers();
		}

		@HandleEvent
		public void serverInitialized(ClusterConnectionManager.ClusterInitializedEvent event) {
		    if (serverManager != null) {
				serverManager.delayStartup = false;
			}
			startServers();
		}

		protected void startServers() {
			if (httpServer == null) {
				if (!serverManager.delayStartup) {
					log.log(Level.INFO, () -> "Starting listening on port " + getPort() + " of HTTP server");
					try {
						httpServer = serverManager.createServer(this);
						httpServer.setExecutor(executor);
						httpServer.start();
						serverManager.registerServer(httpServer);
					} catch (IOException ex) {
						throw new RuntimeException("Could not initialize HTTP server for port " + getPort());
					}
				} else {
					log.log(Level.INFO, () -> "Delaying opening of port " + getPort() + " of HTTP server");
					startupFuture = executor.timer.executor.schedule(() -> {
						if (this.serverManager != null) {
							this.serverManager.delayStartup = false;
						}
						startServers();
						;
					}, 30, TimeUnit.SECONDS);
				}
			}
		}
	}

	@Bean(name = "connections", parent = JavaStandaloneHttpServer.class, active = true, exportable = true)
	public static class PortsConfigBean
			extends AbstractHttpServer.PortsConfigBean {

		@Override
		public Class<?> getDefaultBeanClass() {
			return JavaStandaloneHttpServer.PortConfigBean.class;
		}

	}

	private static class RedirectHandler implements HttpHandler {

		private final String redirectEndpoint;

		public RedirectHandler(String redirectEndpoint) {
			this.redirectEndpoint = redirectEndpoint;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			final String originalRequestHost = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Host"))
					.map(this::parseHostname)
					.orElse("localhost");
			final String requestEndpoint = redirectEndpoint.replace("{host}", originalRequestHost);
			final String requestPath = Optional.ofNullable(exchange.getRequestURI().getRawPath()).orElse("/");
			final String requestQuery = Optional.ofNullable(exchange.getRequestURI().getRawQuery())
					.map(query -> "?" + query)
					.orElse("");
			String uri = requestEndpoint + requestPath + requestQuery;
			exchange.getResponseHeaders().set("Location", uri);
			exchange.sendResponseHeaders(301, -1);
		}

		private String parseHostname(String host) {
			int idx = host.indexOf(":");
			return idx >= 0 ? host.substring(0, idx) : host;
		}
	}
}