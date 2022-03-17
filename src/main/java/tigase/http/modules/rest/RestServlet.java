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

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.WriterOutput;
import gg.jte.resolve.ResourceCodeResolver;
import tigase.http.api.HttpException;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.jaxrs.RequestHandler;
import tigase.http.modules.rest.docs.Endpoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RestServlet
		extends JaxRsServlet<RestHandler, RestModule> {

	private static final Logger log = Logger.getLogger(RestServlet.class.getCanonicalName());
	private final TemplateEngine templateEngine;

	public RestServlet() {
		templateEngine = TemplateEngine.create(new ResourceCodeResolver("tigase/rest"), ContentType.Html);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("GET".equals(req.getMethod())) {
			String context = req.getContextPath();
			if (context.endsWith("/")) {
				context = context.substring(0, context.length()-1);
			}
			String path = context + req.getServletPath();
			if (path.equals(req.getRequestURI())) {
				handleIndexRequest(req, resp);
				return;
			}
		}
		super.service(req, resp);
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

	protected void handleIndexRequest(HttpServletRequest req, HttpServletResponse resp) {
		try {
			WriterOutput output = new WriterOutput(resp.getWriter());
			Map<String, Object> context = new HashMap<>();
			context.put("endpoints", getEndpoints());
			String restBaseUrl = req.getRequestURL().toString();
			if (restBaseUrl.endsWith("/")) {
				restBaseUrl = restBaseUrl.substring(0, restBaseUrl.length() - 1);
			}
			context.put("restBaseUrl", restBaseUrl);
			templateEngine.render("index.jte", context, output);
		} catch (IOException ex) {
			try {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} catch (Throwable ex1) {
				// nothing to do..
			}
		}
	}

	protected List<Endpoint> getEndpoints() {
		return requestHandlers.values()
				.stream()
				.flatMap(List::stream)
				.map(RequestHandler::getMethod)
				.map(Endpoint::create)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(Endpoint::getPath)
								.thenComparingInt(endpoint -> endpoint.getMethod().ordinal()))
				.collect(Collectors.toList());
	}

}
