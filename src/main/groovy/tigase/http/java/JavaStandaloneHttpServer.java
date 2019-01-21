/**
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

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import tigase.http.DeploymentInfo;
import tigase.http.api.HttpServerIfc;
import tigase.io.TLSUtil;
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
public class JavaStandaloneHttpServer implements HttpServerIfc {

	private static final Logger log = Logger.getLogger(JavaStandaloneHttpServer.class.getCanonicalName());
	
	private final Set<HttpServer> servers = new HashSet<HttpServer>();
	private int[] ports = { DEF_HTTP_PORT_VAL };
	private final Map<String,Map<String,Object>> portsConfigs = new HashMap<>();
	private List<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();
	private ExecutorWithTimeout executor = new ExecutorWithTimeout();
	private Timer timer = null;
	
	@Override
	public void start() {
		synchronized (servers) {
			if (!servers.isEmpty())
				stop();

			timer = new Timer();
			executor.start();
			for (int port : ports) {
				try {
					HttpServer server = createServer(port);
					server.setExecutor(executor);
					server.start();
					deploy(server, Collections.unmodifiableList(deployments));
					servers.add(server);
				} catch (IOException ex) {
					log.log(Level.SEVERE, "starting server on port " + port + " failed", ex);
				}
			}
		}
	}

	@Override
	public void stop() {
		synchronized (servers) {
			for (HttpServer server : servers) {
				undeploy(server,Collections.unmodifiableList(deployments));
				server.stop(1);
			}
			servers.clear();
			executor.shutdown();
			timer.cancel();
			timer = null;
		}
	}

	@Override
	public void deploy(DeploymentInfo deployment) {
		deployments.add(deployment);
		synchronized (servers) {
			for (HttpServer server : servers) {
				deploy(server, Collections.singletonList(deployment));
			}
		}
	}

	@Override
	public void undeploy(DeploymentInfo deployment) {
		deployments.remove(deployment);
		synchronized (servers) {
			for (HttpServer server : servers) {
				undeploy(server, Collections.singletonList(deployment));
			}
		}
	}
	
	@Override
	public void setProperties(Map<String, Object> props) {
		if (props.containsKey(HTTP_PORT_KEY)) {
			ports = new int[] { (Integer) props.get(HTTP_PORT_KEY) };
		}
		if (props.containsKey(HTTP_PORTS_KEY)) {
			ports = (int[]) props.get(HTTP_PORTS_KEY);
		}
		portsConfigs.clear();
		for (int port : ports) {
			Map<String,Object> config = new HashMap<>();
			String socket = (String) props.get(String.valueOf(port) + "/" + PORT_SOCKET_KEY);
			config.put(PORT_SOCKET_KEY, socket != null ? SocketType.valueOf(socket) : SocketType.plain);
			String domain = (String) props.get(String.valueOf(port) + "/" + PORT_DOMAIN_KEY);
			config.put(PORT_DOMAIN_KEY, domain);
			portsConfigs.put(String.valueOf(port), config);
		}
		executor.setProperties(props);
	}
	
	private HttpServer createServer(int port) throws IOException {
		Map<String,Object> config = portsConfigs.get(String.valueOf(port));
		if (config == null || ((SocketType) config.get(PORT_SOCKET_KEY)) == SocketType.plain) {
			return HttpServer.create(new InetSocketAddress(port), 100);
		} else {	
			HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 100);
			String domain = (String) config.get(PORT_DOMAIN_KEY);
			SSLContext sslContext = TLSUtil.getSSLContext("TLS", domain);
			server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
			return server;
		}
	}
	
	private void deploy(HttpServer server, List<DeploymentInfo> toDeploy) {
		for (DeploymentInfo info : toDeploy) {
			server.createContext(info.getContextPath(), new RequestHandler(info, timer));
		}
	}
	
	private void undeploy(HttpServer server, List<DeploymentInfo> toUndeploy) {
		for (DeploymentInfo info : toUndeploy) {
			try {
				server.removeContext(info.getContextPath());
			} catch (IllegalArgumentException ex) {
				log.log(Level.FINEST, "deployment context " + info.getContextPath() + " already removed");
			}
		}
	}
		
	private class ExecutorWithTimeout implements Executor {

		private static final String THREADS_KEY = "threads";
		private static final String REQUEST_TIMEOUT_KEY = "request-timeout";

		private ExecutorService executor = null;
		private int timeout = 60 * 1000;

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
		
		public void start() {
			if (executor != null) {
				shutdown();
			}
			executor = Executors.newFixedThreadPool(threads, r -> {
				Thread t = new Thread(r);
				t.setName("http-server-pool-" + counter.incrementAndGet());
				t.setDaemon(true);
				return t;
			});
		}
		
		public void shutdown() {
			executor.shutdown();
		}
		
		public void setProperties(Map<String,Object> props) {
			if (props.containsKey(THREADS_KEY)) {
				threads = (Integer) props.get(THREADS_KEY);
			}
			if (props.containsKey(REQUEST_TIMEOUT_KEY)) {
				timeout = (Integer) props.get(REQUEST_TIMEOUT_KEY);
			}
		}
	}
}
