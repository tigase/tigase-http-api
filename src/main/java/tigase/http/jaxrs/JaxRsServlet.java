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
import tigase.http.java.filters.ForwardedPrefixFilter;
import tigase.http.jaxrs.annotations.LoginForm;
import tigase.http.modules.AbstractBareModule;
import tigase.http.modules.rest.OldGroovyRequestHandler;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

@MultipartConfig
public class JaxRsServlet<M extends JaxRsModule>
		extends HttpServlet {

	public static final Logger log = Logger.getLogger(JaxRsServlet.class.getName());
	
	public static Comparator<RequestHandler> REQUEST_HANDLER_COMPARATOR = Comparator.comparing(requestHandler -> {
		if (requestHandler instanceof JaxRsRequestHandler) {
			return 1;
		}
		if (requestHandler instanceof OldGroovyRequestHandler) {
			return 2;
		}
		return 3;
	});
	public static final String MODULE_KEY = "module-uuid";
	protected ScheduledExecutorService executorService;
	protected M module;
	protected ConcurrentHashMap<HttpMethod, CopyOnWriteArrayList<RequestHandler>> requestHandlers = new ConcurrentHashMap<>();
	private String loginFormPath;

	protected boolean canAccess(RequestHandler requestHandler, HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, ServletException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Checking authentication for request: " + requestHandler + " :: " + request.getRequestURI() + "; contextPath: " + request.getContextPath());
		}
		if (requestHandler.isAuthenticationRequired()) {
			if (((requestHandler.getRequiredRole() != null && requestHandler.getRequiredRole() != Handler.Role.None) &&
					!request.isUserInRole(requestHandler.getRequiredRole().name().toLowerCase())) ||
					(requestHandler.getAllowedRoles() != null &&
							requestHandler.getAllowedRoles().stream().noneMatch(request::isUserInRole))) {
				if (request.getUserPrincipal() != null) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not allowed to access this resource");
				} else {
					if (loginFormPath != null && Optional.ofNullable(request.getHeader("Accept"))
							.stream()
							.flatMap(str -> Arrays.stream(str.split(",")))
							.anyMatch(part -> part.contains("text/html"))) {
						// we have a login form and request is from the browser, so redirect to it...
						var redirectLocation = request.getContextPath() + loginFormPath;
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Redirecting to login form: " + redirectLocation);
						}
						response.sendRedirect(redirectLocation);
					} else {
						response.setHeader("WWW-Authenticate", "Basic realm=\"TigasePlain\"");
						request.authenticate(response);
					}
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
			log.info("for module " + module.getName() + " / " + module.getClass().getSimpleName() +
					         " registering handlers " +
					         handlers.stream().map(Object::getClass).map(Class::getSimpleName).toList());
			for (Handler handler : handlers) {
				registerHandlers(JaxRsRequestHandler.create(handler));
			}
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Handling request: " + req.getMethod() + " :: " + req.getRequestURI() + "; contextPath: " + req.getContextPath() + ", request: " + req);
		}
		try {
			HttpMethod httpMethod = HttpMethod.valueOf(req.getMethod());
			List<RequestHandler> handlers = requestHandlers.get(httpMethod);
			if (handlers != null) {
				String requestUri = req.getRequestURI();
				String contextPath = req.getContextPath();
				if (req instanceof ForwardedPrefixFilter.PrefixedContextPathRequest forwardedRequest) {
					contextPath = forwardedRequest.getOriginalContextPath();
				}
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Handling request: " + req.getMethod() + " :: " + requestUri + "; contextPath: "
							+ contextPath + ", request.contextPath" + req.getContextPath() + ", request: " + req);
				}
				if (!contextPath.equals("/")) {
					if (!contextPath.isEmpty()) {
						requestUri = requestUri.substring(contextPath.length());
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Setting request URI from: " + req.getRequestURI() + " to: " + requestUri);
						}
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
					log.finest("Request URI is empty, setting to root path");
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
			throw new HttpException(ex.getMessage(), HttpServletResponse.SC_NOT_ACCEPTABLE, ex);
		}
	}

	protected void registerHandlers(Collection<? extends RequestHandler> requestHandlers) {
		for (RequestHandler requestHandler : requestHandlers) {
			registerHandler(requestHandler);
		}
	}

	protected void registerHandler(RequestHandler requestHandler) {
		List<RequestHandler> handlers = requestHandlers.computeIfAbsent(requestHandler.getHttpMethod(), x -> new CopyOnWriteArrayList<>());
		handlers.add(requestHandler);
		handlers.sort(Comparator.comparing(Function.identity()));
		if (requestHandler instanceof JaxRsRequestHandler) {
			JaxRsRequestHandler jaxRsRequestHandler = (JaxRsRequestHandler) requestHandler;
			if (jaxRsRequestHandler.getMethod().isAnnotationPresent(LoginForm.class)) {
				// this is default login URL to which we should redirect for authentication with forms
				loginFormPath = jaxRsRequestHandler.getPattern().pattern();
				log.log(Level.CONFIG, "Registering login form for " + loginFormPath);
			}
		}
	}
	
}
