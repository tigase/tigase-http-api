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
package tigase.http.java;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.http.DeploymentInfo;
import tigase.http.api.HttpServerIfc;

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

	private HttpServer server = null;
	private int port = DEF_HTTP_PORT_VAL;
	private List<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();
	private ExecutorWithTimeout executor = new ExecutorWithTimeout();
	
	@Override
	public void start() {
		if (server == null) {
			try {
				executor.start();
				server = HttpServer.create(new InetSocketAddress(port), 100);
				server.setExecutor(executor);
				server.start();
				deploy(Collections.unmodifiableList(deployments));
			} catch (IOException ex) {
				Logger.getLogger(JavaStandaloneHttpServer.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	@Override
	public void stop() {
		if (server != null) {
			undeploy(Collections.unmodifiableList(deployments));
			server.stop(1);
			server = null;
			executor.shutdown();
		}
	}

	@Override
	public void deploy(DeploymentInfo deployment) {
		deployments.add(deployment);
		if (server != null) {
			deploy(Collections.singletonList(deployment));
		}
	}

	@Override
	public void undeploy(DeploymentInfo deployment) {
		deployments.remove(deployment);
		if (server != null) {
			undeploy(Collections.singletonList(deployment));
		}
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		if (props.containsKey(HTTP_PORT_KEY)) {
			port = (Integer) props.get(HTTP_PORT_KEY);
		}
		executor.setProperties(props);
	}
	
	private void deploy(List<DeploymentInfo> toDeploy) {
		for (DeploymentInfo info : toDeploy) {
			server.createContext(info.getContextPath(), new RequestHandler(info));
		}
	}
	
	private void undeploy(List<DeploymentInfo> toUndeploy) {
		for (DeploymentInfo info : toUndeploy) {
			server.removeContext(info.getContextPath());
		}
	}
	
	private class ExecutorWithTimeout implements Executor {

		private static final String THREADS_KEY = "threads";
		private static final String REQUEST_TIMEOUT_KEY = "request-timeout";
		
		private ExecutorService executor = null;
		private Timer timer = null;
		private int timeout = 60 * 1000;
		
		private int threads = 4;
		
		public ExecutorWithTimeout() {
		}
		
		@Override
		public void execute(final Runnable command) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					final Thread current = Thread.currentThread();
					TimerTask tt = new TimerTask() {
						@Override
						public void run() {
							System.out.println("request processing time exceeded!");
							current.interrupt();
						}
					};
					timer.schedule(tt, timeout);
					command.run();
					tt.cancel();
				}
				
			});
		}
		
		public void start() {
			if (executor != null) {
				shutdown();
			}
			executor = Executors.newFixedThreadPool(threads);
			timer = new Timer();
		}
		
		public void shutdown() {
			executor.shutdown();
			timer.cancel();
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
