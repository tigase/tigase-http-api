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
package tigase.http.modules.rest

import groovy.transform.CompileStatic
import tigase.http.jaxrs.Handler
import tigase.http.api.HttpException
import tigase.http.jaxrs.HttpMethod
import tigase.http.api.Service
import tigase.http.api.rest.RestHandler
import tigase.http.coders.Coder
import tigase.http.coders.JsonCoder
import tigase.http.coders.XmlCoder
import tigase.http.jaxrs.RequestHandler
import tigase.http.util.StringUtils
import tigase.xmpp.jid.BareJID

import javax.servlet.AsyncContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ScheduledExecutorService
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class OldGroovyRequestHandler implements RequestHandler {

	private static final Logger log = Logger.getLogger(OldGroovyRequestHandler.class.getCanonicalName());
	private static final int logLenghtLimit = 5120

	public static List<OldGroovyRequestHandler> create(Service<RestModule> service, tigase.http.rest.Handler handler, String prefix, OldGroovyResultEncoder resultEncoder) {
		Handler.Role requiredRole = Handler.Role.None;
		if (handler.requiredRole != null) {
			requiredRole = Handler.Role.valueOf(handler.requiredRole.capitalize());
		}
		String patternStr = (handler.regex instanceof String) ? ((String) handler.regex) : ((Object) handler.regex).toString();
		Pattern pattern = Pattern.compile("/" + prefix + patternStr);
		List<OldGroovyRequestHandler> handlers = [];
		if (handler.execGet != null) {
		 	handlers.add(new OldGroovyRequestHandler(service, handler, (Closure) handler.execGet, HttpMethod.GET, pattern, requiredRole, resultEncoder))
		}
		if (handler.execPost != null) {
			handlers.add(new OldGroovyRequestHandler(service, handler, (Closure) handler.execPost, HttpMethod.POST, pattern, requiredRole, resultEncoder))
		}
		if (handler.execPut != null) {
			handlers.add(new OldGroovyRequestHandler(service, handler, (Closure) handler.execPut, HttpMethod.PUT, pattern, requiredRole, resultEncoder))
		}
		if (handler.execDelete != null) {
			handlers.add(new OldGroovyRequestHandler(service, handler, (Closure) handler.execDelete, HttpMethod.DELETE, pattern, requiredRole, resultEncoder))
		}
		return handlers;
	}

	private final Service<RestModule> service;
	private final Handler.Role requiredRole;
	private final RestHandler handler;
	private final tigase.http.rest.Handler restHandler;
	private final Closure closure;
	private final HttpMethod httpMethod;
	private final Pattern pattern;
	private final Coder xmlCoder = new XmlCoder();
	private final Coder jsonCoder = new JsonCoder();
	private final OldGroovyResultEncoder resultEncoder;

	OldGroovyRequestHandler(Service<RestModule> service, tigase.http.rest.Handler handler, Closure closure, HttpMethod httpMethod, Pattern pattern, Handler.Role requiredRole, OldGroovyResultEncoder resultEncoder) {
		this.service = service;
		this.restHandler = handler;
		this.closure = closure;
		this.httpMethod = httpMethod;
		this.pattern = pattern;
		this.requiredRole = requiredRole;
		this.resultEncoder = resultEncoder;
		RestHandler.Security security = handler.apiKey != null ? RestHandler.Security.ApiKey : RestHandler.Security.None;
		this.handler = new RestHandler() {

			@Override
			RestHandler.Security getSecurity() {
				return security;
			}

			@Override
			Handler.Role getRequiredRole() {
				return requiredRole;
			}
		}
	}
	
	@Override
	Handler getHandler() {
		return handler;
	}

	@Override
	HttpMethod getHttpMethod() {
		return httpMethod;
	}

	@Override
	Handler.Role getRequiredRole() {
		return requiredRole;
	}

	@Override
	Matcher test(HttpServletRequest request, String requestUri) {
		return pattern.matcher(requestUri);
	}

	@Override
	void execute(HttpServletRequest request, HttpServletResponse response, Matcher matcher,
				 ScheduledExecutorService executorService) throws HttpException, IOException {
		def params = matcher[0];
		// first element is uri - removing
		if (params instanceof String) {
			params = [ ]
		} else {
			((List)params).remove(0)
		};

//		def fullPath = request.getRequestURI();
//		def host = request.getServerName();
//		if (handler.apiKey && !service.isAllowed(apiKey, host, fullPath)) {
//			response.sendError(HttpServletResponse.SC_FORBIDDEN,
//							   "To access URI = '" + fullPath + "' a valid api key is required");
//			return;
//		}

		// if authentication is required check if user is in proper role
//		if (handler.authRequired(apiKey) &&
//				(!request.isUserInRole(handler.requiredRole) && !request.authenticate(response))) {
//			handled = true;
//			return;
//		}

		// prepare for execution
		if (restHandler.isAsync) {
			executeAsync(request, response, restHandler, (List) params);
		} else {
			execute(request, response, restHandler, (List) params, null);
		}
	}

	def executeAsync(HttpServletRequest request, HttpServletResponse response, tigase.http.rest.Handler route, List reqParams) {
		AsyncContext asyncCtx = request.startAsync(request, response);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Async context timeout: ${asyncCtx.getTimeout()} for request: ${request}")
		}
		execute(asyncCtx.getRequest() as HttpServletRequest, asyncCtx.getResponse() as HttpServletResponse, route, reqParams, asyncCtx);
	}

	def execute(HttpServletRequest request, HttpServletResponse response, tigase.http.rest.Handler route, List reqParams, AsyncContext asyncCtx) {
		long start = System.currentTimeMillis();
		def prefix = request.getServletPath();
		prefix = request.getContextPath() + prefix

		String type = request.getContentType();
		//service.getModule().countRequest(request);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					request.toString() + ", starting execution of handler: " + route.getClass().getCanonicalName() +
							" for method " + request.getMethod());
		}

		def callback = { result ->
			long end = System.currentTimeMillis();
			executedIn(prefix + route.regex, end - start);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, request.toString() + ", got results for request: " + result);
			}
			if (result == null) {
				// no response - nothing to send so there was nothing
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} else {
				// handle result
				if (result instanceof Closure) {
					// we want to handle request/response - a lot of data to handle (streaming)
					result(request, response);
				} else if (result instanceof String) {
					// send result string
					response.getWriter().write(result);
				} else if (result instanceof byte[]) {
					// send bytes of data
					response.getOutputStream().write(result);
				} else if (result instanceof tigase.http.rest.Handler.Result) {
					// send response with set type and data
					response.setContentType(result.contentType);
					response.setContentLength(result.data.length);
					response.getOutputStream().write(result.data);
				} else {
					encodeResults(request, response, route, reqParams, result);
				}
			}

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "${request}, execution of request in servlet completed");
			}
			if (asyncCtx) {
				asyncCtx.complete();
			}
		}

		def params = [ service, callback ];

		if (route.requiredRole != null) {
			params.add(request.getUserPrincipal() ? BareJID.bareJIDInstance(request.getUserPrincipal().getName()) :
					   null);
		}

		boolean requestAdded = false;
		Class[] paramTypes = closure.getParameterTypes();
		if (paramTypes.length > params.size()) {
			Class expParamType = paramTypes[params.size()];
			if ((Object.class != expParamType) && expParamType.isAssignableFrom(HttpServletRequest.class)) {
				params.add(request);
				requestAdded = true;
			}
		}

		if (type != null && request.getContentLength() > 0) {
			if (route.decodeContent && (type.contains("/xml") || type.contains("/json"))) {
				String requestContent = request.getReader().getText()

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "${request}, received content = ${StringUtils.trimString(requestContent, logLenghtLimit)} of type = ${type}")
				}

				def parsed = null;

				// decoding request content
				if (type.contains("json")) {
					parsed = jsonCoder.decode(requestContent);
				} else {
					parsed = xmlCoder.decode(requestContent);
				}

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "${request}, parsed received content =  ${StringUtils.trimString(parsed, logLenghtLimit)}")
				}

				params.add(parsed)
			} else if (!requestAdded) {
				// pass request if we have content but it is none of JSON or XML
				// or handler requires not decoded content
				params.add(request);
			}
		}

		params.addAll(reqParams)

		if (log.isLoggable(Level.FINEST)) {
			log.finest("${request}, calling handlers `${route.getClass().getCanonicalName()}`, method: `${request.getMethod()}` with params = ${StringUtils.trimString(params, logLenghtLimit)}")
		}

		// Call exact closure
		closure.call(params);
	}

	def executedIn(String route, long executionTime) {
		service.getModule().executedIn(route, executionTime)
	}

	def encodeResults(HttpServletRequest request, HttpServletResponse response, tigase.http.rest.Handler route, def reqParams,
					  def result) {
		String expContentType = resultEncoder.getExpectedContentType(request);
		if ("text/html".equals(expContentType) || "application/x-www-form-urlencoded".equals(expContentType)) {
			// expected output is HTML
			if (resultEncoder.canEncodeResults(route, request.getMethod())) {
				resultEncoder.encodeResults(request, response, route, result);
				return;
			}
		}
		// send output data enconded with XML or JSON
		String type = request.getContentType();
		String output = null;
		type = request.getContentType() ?:
			   (request.getContentType() ?: (request.getParameter("type") ?: "application/xml"));
		if (type.contains("application/json")) {
			response.setContentType("application/json");
			output = jsonCoder.encode(result);
		} else {
			response.setContentType("application/xml");
			output = xmlCoder.encode(result);
		}
		response.getWriter().write(output);
	}

	@Override
	int compareTo(RequestHandler o) {
		if (o instanceof OldGroovyRequestHandler) {
			return PATTERN_COMPARATOR.compare(pattern, ((OldGroovyRequestHandler) o).pattern);
		}
		return Integer.MAX_VALUE;
	}

}
