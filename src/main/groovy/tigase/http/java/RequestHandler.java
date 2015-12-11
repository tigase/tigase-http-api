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
package tigase.http.java;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import tigase.http.DeploymentInfo;
import tigase.http.ServletInfo;
import tigase.http.api.Service;

/**
 *
 * @author andrzej
 */
public class RequestHandler implements HttpHandler {

	private final String contextPath;
	private final Map<String,HttpServlet> servlets = new ConcurrentHashMap<String,HttpServlet>();
	private final Service service;
	
	public RequestHandler(DeploymentInfo info) {
		contextPath = info.getContextPath();
		service = info.getService();
		ServletInfo[] servletInfos = info.getServlets();
		for (ServletInfo servletInfo : servletInfos) {
			registerServlet(servletInfo);
		}
	}
	
	@Override
	public void handle(HttpExchange he) throws IOException {
		String path = he.getRequestURI().getPath();
		List<String> keys = new ArrayList<String>(servlets.keySet());
		for (String key : keys) {
			if (path.startsWith(key)) {
				HttpServlet servlet = servlets.get(key);
				if (servlet != null) {
					try {
						String servletPath = key.substring(contextPath.length(), key.length()-1);
						DummyServletRequest req = new DummyServletRequest(he, contextPath, servletPath, service);
						DummyServletResponse resp = new DummyServletResponse(he); 
						servlet.service(req, resp);
						AsyncContext async = req.getAsyncContext();
						if (async == null) {
							resp.flushBuffer();
							he.getResponseBody().close();
						}
					} catch (ServletException ex) {
						Logger.getLogger(RequestHandler.class.getName()).log(Level.FINE, null, ex);
					}
				}
				break;
			}
		}
	}
	
	private void registerServlet(ServletInfo info) {
		try {
			HttpServlet servlet = info.getServletClass().newInstance();
			ServletConfig cfg = new ServletCfg(info.getInitParams());
			servlet.init(cfg);
			for (String mapping : info.getMappings()) {
				servlets.put(contextPath + mapping.replace("/*", "/"), servlet);
			}
		} catch (Exception ex) {
			Logger.getLogger(RequestHandler.class.getName()).log(Level.WARNING, null, ex);
		}
	}
	
	private class ServletCfg implements ServletConfig {

		private final Map<String,String> params = new HashMap<String,String>();
		
		public ServletCfg(Map<String,String> map) {
			params.putAll(map);
		}
		
		@Override
		public String getServletName() {
			return null;
		}

		@Override
		public ServletContext getServletContext() {
			return null;
		}

		@Override
		public String getInitParameter(String string) {
			return params.get(string);
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return null;
		}
		
	}
}
