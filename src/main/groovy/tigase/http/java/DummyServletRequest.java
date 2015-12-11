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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.http.api.Service;
import tigase.util.Base64;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;

/**
 *
 * @author andrzej
 */
public class DummyServletRequest implements HttpServletRequest {

	private final HttpExchange exchange;
	private final Map<String,String> params;
	
	private final String servletPath;
	private final String contextPath;
	private final Service service;
	private AsyncContext async;
	private Principal principal;
	
	public DummyServletRequest(HttpExchange exchange, String contextPath, String servletPath, Service service) {
		this.exchange = exchange;
		this.params = new HashMap<String,String>();
		String query = exchange.getRequestURI().getRawQuery();
		if (query != null) {
			for (String part : query.split("&")) {
				String[] val = part.split("=");
				try {
					params.put(URLDecoder.decode(val[0], "UTF-8"), val.length == 1 ? "" : URLDecoder.decode(val[1], "UTF-8"));
				} catch (UnsupportedEncodingException ex) {
					Logger.getLogger(DummyServletRequest.class.getName()).log(Level.FINE, "could not decode URLEncoded paramters", ex);
				}
			}
		}
		this.contextPath = contextPath;
		this.servletPath = servletPath;
		this.service = service;
	}
	
	@Override
	public Object getAttribute(String string) {
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public void setCharacterEncoding(String string) throws UnsupportedEncodingException {
	}

	@Override
	public int getContentLength() {
		String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
		if (contentLength == null || contentLength.isEmpty())
			return 0;
		return Integer.parseInt(contentLength);
	}

	@Override
	public String getContentType() {
		return exchange.getRequestHeaders().getFirst("Content-Type");
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ServletInputStream() {
			@Override
			public int read() throws IOException {
				return exchange.getRequestBody().read();
			}

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener rl) {
			}
		};
	}

	@Override
	public String getParameter(String string) {
		return params.get(string);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return null;
	}

	@Override
	public String[] getParameterValues(String string) {
		return null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return null;
	}

	@Override
	public String getProtocol() {
		return exchange.getProtocol();
	}

	@Override
	public String getScheme() {
		return exchange.getProtocol();
	}

	@Override
	public String getServerName() {
		return exchange.getRequestHeaders().getFirst("Host");
	}

	@Override
	public int getServerPort() {
		return exchange.getLocalAddress().getPort();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
	}

	@Override
	public String getRemoteAddr() {
		return exchange.getRemoteAddress().toString();
	}

	@Override
	public String getRemoteHost() {
		return exchange.getRemoteAddress().getHostName();
	}

	@Override
	public void setAttribute(String string, Object o) {
	}

	@Override
	public void removeAttribute(String string) {
	}

	@Override
	public Locale getLocale() {
		return null;
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return null;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String string) {
		return null;
	}

	@Override
	public String getRealPath(String string) {
		return null;
	}

	@Override
	public int getRemotePort() {
		return exchange.getRemoteAddress().getPort();
	}

	@Override
	public String getLocalName() {
		return exchange.getRequestURI().getHost();
	}

	@Override
	public String getLocalAddr() {
		return exchange.getLocalAddress().toString();
	}

	@Override
	public int getLocalPort() {
		return exchange.getLocalAddress().getPort();
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest sr, ServletResponse sr1) throws IllegalStateException {
		async = new AsyncContextImpl(sr, sr1, exchange);
		return async;
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		return async;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.REQUEST;
	}

	@Override
	public String getAuthType() {
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		return new Cookie[0];
	}

	@Override
	public long getDateHeader(String string) {
		return 0;
	}

	@Override
	public String getHeader(String string) {
		return exchange.getRequestHeaders().getFirst(string);
	}

	@Override
	public Enumeration<String> getHeaders(String string) {
		return null;
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return null;
	}

	@Override
	public int getIntHeader(String string) {
		return 0;
	}

	@Override
	public String getMethod() {
		return exchange.getRequestMethod();
	}

	@Override
	public String getPathInfo() {
		return null;
	}

	@Override
	public String getPathTranslated() {
		return null;
	}

	@Override
	public String getContextPath() {
		return contextPath;
	}

	@Override
	public String getQueryString() {
		return exchange.getRequestURI().getQuery();
	}

	@Override
	public String getRemoteUser() {
		return null;
	}

	@Override
	public boolean isUserInRole(String role) {
		Principal principal = getUserPrincipal();
		if ("user".equals(role))
			return principal != null;
		try {
			if ("admin".equals(role))
				return principal != null && service.isAdmin(BareJID.bareJIDInstance(principal.getName()));
		}
		catch (Exception ex) {}
		return false;
	}

	@Override
	public Principal getUserPrincipal() {
		if (principal == null) {
			String authStr = getHeader("Authorization");
			if (authStr != null && authStr.startsWith("Basic")) {
				authStr = authStr.replace("Basic ", "");
				authStr = new String(Base64.decode(authStr));
				int idx = authStr.indexOf(":");
				String user = authStr.substring(0, idx);
				String pass = authStr.substring(idx + 1);
				try {
					if (service.getAuthRepository().plainAuth(BareJID.bareJIDInstance(user), pass)) {
						final String jid = user;
						principal = new Principal() {
							@Override
							public String getName() {
								return jid;
							}
						};						
					}
				} catch (TigaseStringprepException|TigaseDBException|AuthorizationException ex) {
					Logger.getLogger(DummyServletRequest.class.getName()).log(Level.FINE, "could not authorize user", ex);
				}
			}
		}
		return principal;
	}

	@Override
	public String getRequestedSessionId() {
		return null;
	}

	@Override
	public String getRequestURI() {
		String uri = exchange.getRequestURI().toString();
		int idx = uri.indexOf("?");
		if (idx > -1) {
			uri = uri.substring(0, idx);
		}
		return uri;
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer buf = new StringBuffer();
		try {
			buf.append(exchange.getRequestURI().toURL().toExternalForm());
		} catch (MalformedURLException ex) {
			Logger.getLogger(DummyServletRequest.class.getName()).log(Level.FINE, "could not read request URL", ex);
		}
		return buf;
	}

	@Override
	public String getServletPath() {
		return servletPath;
	}

	@Override
	public HttpSession getSession(boolean bln) {
		return null;
	}

	@Override
	public HttpSession getSession() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	@Override
	public boolean authenticate(HttpServletResponse hsr) throws IOException, ServletException {
		hsr.setHeader("WWW-Authenticate", "Basic realm=\"TigasePlain\"");
		hsr.sendError(401, "Not authorized");
		exchange.getResponseBody().flush();
		exchange.getResponseBody().close();
		return false;
	}

	@Override
	public void login(String string, String string1) throws ServletException {
	}

	@Override
	public void logout() throws ServletException {
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return null;
	}

	@Override
	public Part getPart(String string) throws IOException, ServletException {
		return null;
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> type) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public long getContentLengthLong() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
