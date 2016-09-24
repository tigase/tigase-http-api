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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.http.java;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import tigase.http.AbstractHttpServer;
import tigase.http.DeploymentInfo;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.net.SocketType;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic implementation of HTTP server based on HttpServer implementation 
 * embedded in JDK. 
 * 
 * May not fully support HTTP implementation but is sufficient for testing 
 * and basic usage.
 * 
 * @author andrzej
 */
@Bean(name = "httpServer", parent = Kernel.class, exportable = true)
public class JavaStandaloneHttpServer extends AbstractHttpServer {

	private static final Logger log = Logger.getLogger(JavaStandaloneHttpServer.class.getCanonicalName());

	private final Set<HttpServer> servers = new HashSet<HttpServer>();

	private List<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();

	@Inject
	private ExecutorWithTimeout executor;

	@Override
	public void deploy(DeploymentInfo deployment) {
		deployments.add(deployment);
		synchronized (servers) {
			for (HttpServer server : servers) {
				deploy(server);
			}
		}
	}

	@Override
	public void undeploy(DeploymentInfo deployment) {
		deployments.remove(deployment);
		synchronized (servers) {
			for (HttpServer server : servers) {
				undeploy(server);
			}
		}
	}

	@Override
	public List<DeploymentInfo> listDeployed() {
		return Collections.unmodifiableList(deployments);
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

	protected HttpServer createServer(PortConfigBean config) throws IOException {
		if (config.getSocket() == SocketType.plain) {
			return HttpServer.create(new InetSocketAddress(config.getPort()), 100);
		} else {	
			HttpsServer server = HttpsServer.create(new InetSocketAddress(config.getPort()), 100);
			SSLContext sslContext = sslContextContainer.getSSLContext("TLS", config.getDomain(), false);
			server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
			return server;
		}
	}
	
	protected void deploy(HttpServer server) {
		List<DeploymentInfo> toDeploy = Collections.unmodifiableList(deployments);
		for (DeploymentInfo info : toDeploy) {
			server.createContext(info.getContextPath(), new RequestHandler(info, executor.timer));
		}
	}
	
	protected void undeploy(HttpServer server) {
		List<DeploymentInfo> toUndeploy = Collections.unmodifiableList(deployments);
		for (DeploymentInfo info : toUndeploy) {
			try {
				server.removeContext(info.getContextPath());
			} catch (IllegalArgumentException ex) {
				log.log(Level.FINEST, "deployment context " + info.getContextPath() + " already removed");
			}
		}
	}

	@Bean(name="executor", parent = JavaStandaloneHttpServer.class, exportable = true)
	public static class ExecutorWithTimeout implements Executor, Initializable, UnregisterAware, ConfigurationChangedAware {

		private static final String THREADS_KEY = "threads";
		private static final String REQUEST_TIMEOUT_KEY = "request-timeout";

		private ExecutorService executor = null;
		private Timer timer = null;
		@ConfigField(desc = "Request timeout", alias = REQUEST_TIMEOUT_KEY)
		private int timeout = 60 * 1000;
		@ConfigField(desc = "Number of threads", alias = THREADS_KEY)
		private int threads = 4;
		private AtomicInteger counter = new AtomicInteger(0);
		
		public ExecutorWithTimeout() {
		}
		
		@Override
		public void execute(final Runnable command) {
			executor.execute(() -> {
				RequestHandler.setExecutionTimeout(timeout);

				command.run();
			});
		}

		@Override
		public void beforeUnregister() {
			executor.shutdown();
			timer.cancel();
		}

		@Override
		public void initialize() {
			if (executor != null) {
				beforeUnregister();
			}
			timer = new Timer();
			executor = Executors.newFixedThreadPool(threads, r -> {
				Thread t = new Thread(r);
				t.setName("http-server-pool-" + counter.incrementAndGet());
				t.setDaemon(true);
				return t;
			});
		}

		@Override
		public void beanConfigurationChanged(Collection<String> changedFields) {
			initialize();
		}
	}

	@Bean(name = "connections", parent = JavaStandaloneHttpServer.class, exportable = true)
	public static class PortsConfigBean extends AbstractHttpServer.PortsConfigBean {

		@Override
		public Class<?> getDefaultBeanClass() {
			return JavaStandaloneHttpServer.PortConfigBean.class;
		}

	}

	public static class PortConfigBean extends AbstractHttpServer.PortConfigBean {

		@Inject
		private JavaStandaloneHttpServer serverManager;

		@Inject(bean = "executor")
		private ExecutorWithTimeout executor;

		protected HttpServer httpServer;

		public void beanConfigurationChanged(Collection<String> changedFields) {
			if (serverManager == null)
				return;

			beforeUnregister();
			initialize();
		}

		@Override
		public void beforeUnregister() {
			if (httpServer != null) {
				serverManager.unregisterServer(httpServer);
				httpServer.stop(1);
				httpServer = null;
			}
		}

		@Override
		public void initialize() {
			if (httpServer == null) {
				try {
					httpServer = serverManager.createServer(this);
					httpServer.setExecutor(executor);
					httpServer.start();
					serverManager.registerServer(httpServer);
				} catch (IOException ex) {
					throw new RuntimeException("Could not initialize HTTP server for port " + getPort());
				}
			}
		}
	}
}
