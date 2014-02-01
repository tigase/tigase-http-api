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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class HttpRegistratorInt extends HttpRegistrator {

	private final Server server;
	private final ContextHandlerCollection contexts;
	
	public HttpRegistratorInt(Server server) {
		this.server = server;
		this.contexts = new ContextHandlerCollection();
		server.setHandler(contexts);
	}
	
	@Override
	public void registerContext(ServletContextHandler ctx) {
		contexts.addHandler(ctx);
	}

	@Override
	public void unregisterContext(ServletContextHandler ctx) {
		contexts.removeHandler(ctx);
	}
	
}
