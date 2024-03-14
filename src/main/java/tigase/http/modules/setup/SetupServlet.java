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
package tigase.http.modules.setup;

import tigase.http.jaxrs.JaxRsRequestHandler;
import tigase.http.jaxrs.JaxRsServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class SetupServlet extends JaxRsServlet<SetupModule> {

	public SetupServlet() {
	}

	@Override
	public void init() throws ServletException {
		super.init();
		List<SetupHandler> handlers = module.getHandlers();
		for (SetupHandler handler : handlers) {
			registerHandlers(JaxRsRequestHandler.create(handler));
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ((req.getContextPath() + "/").equals(req.getRequestURI())) {
			String redirectTo = req.getRequestURL().toString() + module.getHandlers()
					.stream()
					.map(SetupHandler::getPath)
					.findFirst()
					.map(path -> path.substring(1))
					.get();
			resp.sendRedirect(redirectTo);
			return;
		}
		super.service(req, resp);
	}
	
}