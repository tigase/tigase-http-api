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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import static tigase.http.api.HttpServerIfc.DEF_HTTP_PORT_VAL;
import static tigase.http.api.HttpServerIfc.HTTP_PORT_KEY;

/**
 * This implementation embeds Jetty HTTP Server by starting separate instance
 * which is configured and managed by Tigase.
 * 
 * @author andrzej
 */
public class JettyStandaloneHttpServer extends AbstractJettyHttpServer {

	private static final Logger log = Logger.getLogger(JettyStandaloneHttpServer.class.getCanonicalName());
	
	private int port = DEF_HTTP_PORT_VAL;
	private Server server = null;
	private final ContextHandlerCollection contexts = new ContextHandlerCollection();
	
	@Override
	protected void deploy(ServletContextHandler ctx) {
		contexts.addHandler(ctx);
	}

	@Override
	protected void undeploy(ServletContextHandler ctx) {
		contexts.removeHandler(ctx);
	}

	@Override
	public void start() {
		if (server != null) {
			stop();
		}
		server = new Server(port);
		server.setHandler(contexts);
	}

	@Override
	public void stop() {
		if (server == null)
			return;
		
		try {
			server.stop();
			server = null;
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception stopping internal HTTP server", ex);
		}
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		if (props.containsKey(HTTP_PORT_KEY)) {
			port = (Integer) props.get(HTTP_PORT_KEY);
		}
	}
	
}
