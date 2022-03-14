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
package tigase.http.modules.rest;

import tigase.http.api.HttpException;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.jaxrs.RequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RestServlet
		extends JaxRsServlet<RestHandler, RestModule> {

	public RestServlet() {
		
	}

	protected void canAccess(RequestHandler<RestHandler> requestHandler, HttpServletRequest request, HttpServletResponse response)
			throws HttpException, IOException, ServletException {
		switch (requestHandler.getHandler().getSecurity()) {
			case None:
				break;
			case ApiKey:
				checkApiKey(request);
		}

		RestHandler.Role requiredRole = requestHandler.getHandler().getRequiredRole();
		if (requiredRole.isAuthenticationRequired()) {
			if (!request.isUserInRole(requiredRole.name().toLowerCase())) {
				request.authenticate(response);
				return;
			}
		}
	}

	protected void checkApiKey(HttpServletRequest request) throws HttpException {
		String apiKey = request.getHeader("Api-Key");
		if (apiKey == null) {
			throw new HttpException("Missing required 'Api-Key' header", HttpServletResponse.SC_FORBIDDEN);
		}

		if (!module.isRequestAllowed(apiKey, request.getServerName(), request.getRequestURI())) {
			throw new HttpException("Provided Api-Key is not authorized to access " + request.getRequestURI(), HttpServletResponse.SC_FORBIDDEN);
		}
	}

}
