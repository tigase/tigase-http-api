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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import static tigase.http.api.HttpServerIfc.DEF_HTTP_PORT_VAL;
import static tigase.http.api.HttpServerIfc.HTTP_PORTS_KEY;
import static tigase.http.api.HttpServerIfc.HTTP_PORT_KEY;
import static tigase.http.api.HttpServerIfc.PORT_DOMAIN_KEY;
import static tigase.http.api.HttpServerIfc.PORT_SOCKET_KEY;
import tigase.io.SSLContextContainerIfc;
import tigase.io.TLSUtil;
import tigase.net.SocketType;

/**
 * This implementation embeds Jetty HTTP Server by starting separate instance
 * which is configured and managed by Tigase.
 * 
 * @author andrzej
 */
public class JettyStandaloneHttpServer extends AbstractJettyHttpServer {

	private static final Logger log = Logger.getLogger(JettyStandaloneHttpServer.class.getCanonicalName());
	
	private Server server = null;
	private int[] ports = { DEF_HTTP_PORT_VAL };
	private final Map<String,Map<String,Object>> portsConfigs = new HashMap<>();
	private final ContextHandlerCollection contexts = new ContextHandlerCollection();
	
	@Override
	protected void deploy(ServletContextHandler ctx) {
		contexts.addHandler(ctx);
		try {
			ctx.start();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception deploying http context " + ctx.getContextPath(), ex);
		}
	}

	@Override
	protected void undeploy(ServletContextHandler ctx) {
		contexts.removeHandler(ctx);
		try {
			ctx.stop();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception undeploying http context " + ctx.getContextPath(), ex);
		}
	}

	@Override
	public void start() {
		if (server != null && (server.isStarted() || server.isStarting())) {
			stop();
		}
		if (server == null) {
			server = new Server();
			for (int port : ports) {
				Map<String,Object> config = portsConfigs.get(String.valueOf(port));
				ServerConnector connector = null;
				if (config == null || ((SocketType) config.get(PORT_SOCKET_KEY)) == SocketType.plain) {
					connector = new ServerConnector(server);
				} else {
					String domain = (String) config.get(PORT_DOMAIN_KEY);
					SSLContext context = TLSUtil.getSSLContext("TLS", domain);
					SslContextFactory contextFactory = new SslContextFactory();
					contextFactory.setSslContext(context);
					connector = new ServerConnector(server, contextFactory);
				}
				connector.setPort(port);
				server.addConnector(connector);				
			}
		}
		server.setHandler(contexts);
		try {
			server.start();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception starting internal HTTP server", ex);
		}
	}

	@Override
	public void stop() {
		if (server == null || !(server.isStarted() || server.isStarting()))
			return;
		
		try {
			server.stop();
			//server.destroy();
			//server = null;
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception stopping internal HTTP server", ex);
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
	}
	
}
