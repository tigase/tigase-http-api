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

import tigase.http.ServiceImpl;
import tigase.http.api.HttpException;
import tigase.http.api.rest.RestHandler;
import tigase.http.jaxrs.Handler;
import tigase.http.jaxrs.JaxRsRequestHandler;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.jaxrs.RequestHandler;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestServlet
		extends JaxRsServlet<RestModule> {

	private static final Logger log = Logger.getLogger(RestServlet.class.getCanonicalName());

	private ServiceImpl<RestModule> service;
	
	@Override
	public void init() throws ServletException {
		super.init();
		List<Handler> handlers = module.getHandlers();
		for (Handler handler : handlers) {
			registerHandlers(JaxRsRequestHandler.create(handler));
		}
		service = new ServiceImpl<>(module);
		initGroovyScripts();
	}

	public final static String SCRIPTS_DIR_KEY = "script-dir";
	private final OldGroovyResultEncoder oldGroovyResultEncoder = new OldGroovyResultEncoder();
	
	private void initGroovyScripts() {
		ServletConfig cfg = getServletConfig();
		File rootScriptsDir = new File(cfg.getInitParameter(SCRIPTS_DIR_KEY));
		File[] scriptDirFiles = rootScriptsDir.listFiles(
				file -> file.isDirectory() && !"static".equals(file.getName()));
		List<tigase.http.rest.Handler> allHandlers = new ArrayList<>();
		if (scriptDirFiles != null) {
			for (File scriptsDir : scriptDirFiles) {
				File[] scriptFiles = RestModule.getGroovyFiles(scriptsDir);
				if (scriptFiles != null) {
					List<tigase.http.rest.Handler> listOfHandlers = HandlersLoader.getInstance().
							loadHandlers(module.getKernel(), Arrays.asList(scriptFiles));

					for (tigase.http.rest.Handler handler : listOfHandlers) {
						this.registerHandlers(OldGroovyRequestHandler.create(service, handler, scriptsDir.getName(), oldGroovyResultEncoder));
					}
					allHandlers.addAll(listOfHandlers);
				}
			}
		}
		oldGroovyResultEncoder.loadTemplates(rootScriptsDir, allHandlers);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("comparing request URI = " + request.getRequestURI() + " with " +
								   (request.getContextPath() + request.getServletPath()));
			}
			String uri = request.getRequestURI();
			if (uri.equals(request.getContextPath()) || uri.equals(request.getContextPath() + "/")) {
				// accessing root of REST service - we should provide info about service here
				Map<String, Object> result = new HashMap<>();
				result.put("service", service);

				oldGroovyResultEncoder.renderIndex(request, response, result);
				return;
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		super.service(request, response);
	}
	
	protected boolean canAccess(RequestHandler requestHandler, HttpServletRequest request, HttpServletResponse response)
			throws HttpException, IOException, ServletException {
		switch (((RestHandler) requestHandler.getHandler()).getSecurity()) {
			case None:
				break;
			case ApiKey:
				checkApiKey(request);
		}

		return super.canAccess(requestHandler, request, response);
	}

	protected void checkApiKey(HttpServletRequest request) throws HttpException {
		String apiKey = request.getHeader("Api-Key");
		// fallback to support old api keys...
		if (apiKey == null) {
			apiKey = request.getParameter("api-key");
		}
		if (!service.isAllowed(apiKey, request.getServerName(), request.getRequestURI())) {
			if (apiKey == null) {
				throw new HttpException("Missing required 'Api-Key' header", HttpServletResponse.SC_FORBIDDEN);
			}
			throw new HttpException("Provided Api-Key is not authorized to access " + request.getRequestURI(), HttpServletResponse.SC_FORBIDDEN);
		}
	}
}
