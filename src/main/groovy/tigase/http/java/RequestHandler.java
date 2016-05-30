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
import tigase.http.DeploymentInfo;
import tigase.http.ServletInfo;
import tigase.http.api.Service;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
public class RequestHandler implements HttpHandler {

	private static final Logger log = Logger.getLogger(RequestHandler.class.getCanonicalName());
	
	private final String contextPath;
	private final Map<String,HttpServlet> servlets = new ConcurrentHashMap<String,HttpServlet>();
	private final Service service;
	
	private static final Comparator<String> COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			int val1 = o1.length() - o1.replace("/", "").length(); // 3
			int val2 = o2.length() - o2.replace("/", "").length(); // 5
			if (val2 != val1)
				return val2 - val1;
			return Integer.compare(o2.length(), o1.length());
		}
	};
	
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
		Collections.sort(keys, COMPARATOR);
		for (String key : keys) {
			if (path.startsWith(key)) {
				HttpServlet servlet = servlets.get(key);
				if (servlet != null) {
					try {
						String servletPath = key.substring(contextPath.length(), key.length());
						if (servletPath.isEmpty())
							servletPath = "/";
						DummyServletRequest req = new DummyServletRequest(he, contextPath, servletPath, service);
						DummyServletResponse resp = new DummyServletResponse(he);
						if (key.endsWith(path) && !key.equals("/")) {
							String query = req.getQueryString();
							if (query == null || query.isEmpty())
								resp.sendRedirect(req.getRequestURI() + "/");
							else
								resp.sendRedirect(req.getRequestURI() + "/?" + query);
							return;
						}
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
				if (mapping.endsWith("/"))
					mapping = mapping.substring(0, mapping.length()-1);
//				if ("/".equals(contextPath)) {
//					servlets.put(mapping.replace("/*", ""), servlet);
//				} else {
				servlets.put(contextPath + mapping.replace("/*", ""), servlet);
//				}
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
