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
package tigase.http.jaxrs;

import tigase.http.api.HttpException;
import tigase.http.jaxrs.annotations.LoginForm;
import tigase.http.modules.AbstractBareModule;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;

public class JaxRsServlet<M extends JaxRsModule>
		extends HttpServlet {

	public static final String MODULE_KEY = "module-uuid";
	protected ScheduledExecutorService executorService;
	protected M module;
	protected ConcurrentHashMap<HttpMethod, CopyOnWriteArrayList<RequestHandler>> requestHandlers = new ConcurrentHashMap<>();
	private String loginFormPath;

	protected boolean canAccess(RequestHandler requestHandler, HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, ServletException {
		Handler.Role requiredRole = requestHandler.getRequiredRole();
		if (requiredRole.isAuthenticationRequired()) {
			if (!request.isUserInRole(requiredRole.name().toLowerCase())) {
				if (loginFormPath != null && Optional.ofNullable(request.getHeader("Accept"))
						.stream()
						.flatMap(str -> Arrays.stream(str.split(",")))
						.anyMatch(part -> part.contains("text/html"))) {
					// we have a login form and request is from the browser, so redirect to it...
					response.sendRedirect(request.getContextPath() + loginFormPath);
				} else {
					response.setHeader("WWW-Authenticate", "Basic realm=\"TigasePlain\"");
					request.authenticate(response);
				}
				return false;
			}
			module.getAuthProvider().refreshJwtToken(request, response);
		}
		return true;
	}
	
	@Override
	public void init() throws ServletException {
		super.init();
		ServletConfig cfg = super.getServletConfig();
		String moduleName = cfg.getInitParameter(MODULE_KEY);
		module = AbstractBareModule.getModuleByUUID(moduleName);
		executorService = module.getExecutorService();
		List<Handler> handlers = module.getHandlers();
		if (handlers != null) {
			for (Handler handler : handlers) {
				registerHandlers(JaxRsRequestHandler.create(handler));
			}
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			HttpMethod httpMethod = HttpMethod.valueOf(req.getMethod());
			List<RequestHandler> handlers = requestHandlers.get(httpMethod);
			if (handlers != null) {
				String requestUri = req.getRequestURI();
				if (!req.getContextPath().equals("/")) {
					if (!req.getContextPath().isEmpty()) {
						requestUri = requestUri.substring(req.getContextPath().length());
					}
				}
				for (RequestHandler handler : handlers) {
					Matcher matcher = handler.test(req, requestUri);
					if (matcher != null && matcher.matches()) {
						if (canAccess(handler, req, resp)) {
							handler.execute(req, resp, matcher, executorService);
						} 
						return;
					}
				}
				if (requestUri.isEmpty()) {
					requestUri = "/";
					for (RequestHandler handler : handlers) {
						Matcher matcher = handler.test(req, requestUri);
						if (matcher != null && matcher.matches()) {
							if (canAccess(handler, req, resp)) {
								handler.execute(req, resp, matcher, executorService);
							}
							return;
						}
					}
				}
			}
			resp.sendError(404, "Not found");
		} catch (ValidationException ex) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, ex.getMessage());
		} catch (HttpException ex) {
			resp.sendError(ex.getCode(), ex.getMessage());
		}
	}

	protected void registerHandlers(Collection<? extends RequestHandler> requestHandlers) {
		for (RequestHandler requestHandler : requestHandlers) {
			registerHandler(requestHandler);
		}
	}

	protected void registerHandler(RequestHandler requestHandler) {
		requestHandlers.computeIfAbsent(requestHandler.getHttpMethod(), x -> new CopyOnWriteArrayList<>()).add(requestHandler);
		if (requestHandler instanceof JaxRsRequestHandler) {
			JaxRsRequestHandler jaxRsRequestHandler = (JaxRsRequestHandler) requestHandler;
			if (jaxRsRequestHandler.getMethod().isAnnotationPresent(LoginForm.class)) {
				// this is default login URL to which we should redirect for authentication with forms
				loginFormPath = jaxRsRequestHandler.getPattern().pattern();
			}
		}
	}
	
}
