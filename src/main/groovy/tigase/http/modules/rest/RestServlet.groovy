/**
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

import tigase.http.ServiceImpl
import tigase.http.coders.Coder
import tigase.http.coders.JsonCoder
import tigase.http.coders.XmlCoder
import tigase.http.rest.Handler
import tigase.http.rest.Service
import tigase.xmpp.jid.BareJID

import javax.servlet.AsyncContext
import javax.servlet.ServletConfig
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.logging.Level
import java.util.logging.Logger

@WebServlet(asyncSupported = true)
public class RestServlet
		extends HttpServlet
		implements RestServletIfc {

	public static String REST_MODULE_KEY = "rest-module-uuid";
	public static String SCRIPTS_DIR_KEY = "script-dir";

	def log = Logger.getLogger(RestServlet.class.getCanonicalName())
	def methods = [ "GET", "POST", "PUT", "DELETE" ];
	def handlers = [ : ];

	Coder xmlCoder = new XmlCoder();
	Coder jsonCoder = new JsonCoder();

	Service<RestModule> service = null;
	File scriptsDir = null;

	@Override
	public void init() {
		super.init()
		ServletConfig cfg = super.getServletConfig();
		String moduleName = cfg.getInitParameter(REST_MODULE_KEY);
		service = new ServiceImpl<RestModule>(moduleName);
		scriptsDir = new File(cfg.getInitParameter(SCRIPTS_DIR_KEY));

		File[] scriptFiles = RestModule.getGroovyFiles(scriptsDir);

		loadHandlers(scriptFiles);

		service.getModule().registerRestServlet(this);
	}

	public void loadHandlers(File[] scriptFiles) {
		if (scriptFiles != null) {
			def listOfHandlers = HandlersLoader.getInstance().
					loadHandlers(service.getModule().getKernel(), scriptFiles.toList());

			def newHandlers = [ : ];
			methods.each { method ->
				newHandlers[method] = listOfHandlers.findAll { it."exec${method.toLowerCase().capitalize()}" != null }
			}

			handlers = newHandlers;

			if (log.isLoggable(Level.INFO)) {
				log.info("loaded ${listOfHandlers.size()} handlers")
			}
		}
	}

	/**
	 * Should return mapping of requests to methods*/
	def getHandlers = { method -> return handlers[method];
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) {
		processRequest(request, response);
	}

	/**
	 * Parse request URI and find closure with matching regex
	 *
	 * @param request
	 * @param response
	 */
	def processRequest(HttpServletRequest request, HttpServletResponse response) {
		String method = request.getMethod();

		def routings = getHandlers(method);

		def prefix = request.getServletPath();

		prefix = request.getContextPath() + prefix

		def apiKey = request.getParameter("api-key") ?: request.getHeader("Api-Key");

		String localUri = URLDecoder.decode(request.getRequestURI(), "UTF-8").replace(prefix, "");

		if (log.isLoggable(Level.FINEST)) {
			log.finest("checking routings = " + routings + " for prefix = " + prefix + " and uri = " + localUri)
		}

		boolean handled = false;
		routings.each { Handler handler ->
			if (log.isLoggable(Level.FINEST)) {
				log.finest("checking localUri = " + localUri + ", prefix = " + prefix + ", regex = " + handler.regex);
			}

			// check if regex matches
			def matcher = (localUri =~ handler.regex)
			if (matcher.matches()) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("found handler")
				}

				def params = matcher[0];
				// first element is uri - removing
				if (params instanceof String) {
					params = [ ]
				} else {
					params.remove(0)
				};

				def fullPath = request.getRequestURI();
				def host = request.getServerName();
				if (handler.apiKey && !service.isAllowed(apiKey, host, fullPath)) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN,
									   "To access URI = '" + fullPath + "' a valid api key is required");
					return;
				}

				// if authentication is required check if user is in proper role
				if (handler.authRequired(apiKey) &&
						(!request.isUserInRole(handler.requiredRole) && !request.authenticate(response))) {
					handled = true;
					return;
				}

				// prepare for execution
				if (handler.isAsync) {
					executeAsync(request, response, handler, (List) params);
				} else {
					execute(request, response, handler, (List) params, null);
				}

				handled = true;
			}
		}

		// if request is not handled return 404
		if (!handled) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("request not handled")
			}

			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * Prepare for execution of async closure
	 *
	 * @param request
	 * @param response
	 * @param route
	 * @param reqParams
	 * @return
	 */
	def executeAsync(HttpServletRequest request, HttpServletResponse response, Handler route, List reqParams) {
		AsyncContext asyncCtx = request.startAsync(request, response);
		execute(asyncCtx.getRequest(), asyncCtx.getResponse(), route, reqParams, asyncCtx);
	}

	/**
	 * Prepare for execution of closure (decode parameters) and execute closure
	 *
	 * @param request
	 * @param response
	 * @param route
	 * @param reqParams
	 * @param asyncCtx
	 * @return
	 */
	def execute(HttpServletRequest request, HttpServletResponse response, Handler route, List reqParams, AsyncContext asyncCtx) {
		long start = System.currentTimeMillis();
		def prefix = request.getServletPath();
		prefix = request.getContextPath() + prefix

		String type = request.getContentType();
		service.getModule().countRequest(request);


		def method = request.getMethod().toLowerCase().capitalize()
		
		def callback = { result ->
			long end = System.currentTimeMillis();
			executedIn(prefix + route.regex.toString(), end - start);
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
				} else if (result instanceof Handler.Result) {
					// send response with set type and data
					response.setContentType(result.contentType);
					response.setContentLength(result.data.length);
					response.getOutputStream().write(result.data);
				} else {
					encodeResults(request, response, route, reqParams, result);
				}
			}

			if (asyncCtx) {
				asyncCtx.complete();
			}
		}

		def params = [ service, callback ];

		if (route.requiredRole != null) {
			params.add(
					request.getUserPrincipal() ? BareJID.bareJIDInstance(request.getUserPrincipal().getName()) : null);
		}

		boolean requestAdded = false;
		Class[] paramTypes = ((Closure) route."exec$method").getParameterTypes();
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
					log.finest("received content = " + requestContent + "of type = " + type)
				}

				def parsed = null;

				// decoding request content
				if (type.contains("json")) {
					parsed = jsonCoder.decode(requestContent);
				} else {
					parsed = xmlCoder.decode(requestContent);
				}

				if (log.isLoggable(Level.FINEST)) {
					log.finest("parsed received content = " + parsed)
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
			log.finest("got calling with params = " + params.toString())
		}

		// Call exact closure
		route."exec$method".call(params);
	}

	def executedIn(String route, long executionTime) {
		service.getModule().executedIn(route, executionTime)
	}

	def encodeResults(HttpServletRequest request, HttpServletResponse response, Handler route, def reqParams,
					  def result) {
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

}
