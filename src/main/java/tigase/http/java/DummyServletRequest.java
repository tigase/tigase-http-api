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
package tigase.http.java;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsServer;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.http.AuthProvider;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author andrzej
 */
public class DummyServletRequest
		implements HttpServletRequest {

	private final String contextPath;
	private final HttpExchange exchange;
	private final Integer executionTimeout;
	private final Map<String, String[]> params;
	private final Cookie[] cookies;
	private final AuthProvider authProvider;
	private final String servletPath;
	private final ScheduledExecutorService timer;
	private AsyncContext async;
	private String characterEncoding = "UTF-8";
	private Principal principal;
	private BufferedReader reader;
	private int requestId;
	private HttpServletResponse response;

	public DummyServletRequest(int requestId, HttpExchange exchange, String contextPath, String servletPath, AuthProvider authProvider,
							   ScheduledExecutorService timer, Integer executionTimeout, HttpServletResponse response) {
		this.requestId = requestId;
		this.exchange = exchange;
		this.authProvider = authProvider;
		this.params = new HashMap<>();
		this.response = response;
		String query = exchange.getRequestURI().getRawQuery();
		if (query != null) {
			decodeParamsFromString(query, params);
		}
		if ("application/x-www-form-urlencoded".equals(getContentType())) {
			int len = getContentLength();
			byte[] data = new byte[len];
			try {
				exchange.getRequestBody().read(data);
				decodeParamsFromString(new String(data), params);
			} catch (IOException ex) {
				Logger.getLogger(DummyServletRequest.class.getName())
						.log(Level.FINE, "could not read parameters from input stream", ex);
			}
		}

		this.cookies = exchange.getRequestHeaders()
				.entrySet()
				.stream()
				.filter(entry -> "Cookie".equalsIgnoreCase(entry.getKey()))
				.flatMap(entry1 -> entry1.getValue().stream())
				.flatMap(item -> Arrays.stream(item.split(";")))
				.map(cookie -> {
					final String[] tokens = cookie.split("=");
					return new Cookie(tokens[0].trim(), tokens[1].trim());
				})
				.toArray(Cookie[]::new);

		this.contextPath = contextPath;
		this.servletPath = servletPath;
		this.timer = timer;
		this.executionTimeout = executionTimeout;
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
		return characterEncoding;
	}

	@Override
	public void setCharacterEncoding(String string) throws UnsupportedEncodingException {
		this.characterEncoding = string;
	}

	@Override
	public int getContentLength() {
		synchronized (exchange) {
			String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
			if (contentLength == null || contentLength.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(contentLength);
		}
	}

	@Override
	public String getContentType() {
		synchronized (exchange) {
			return exchange.getRequestHeaders().getFirst("Content-Type");
		}
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ServletInputStream() {

			private boolean finished = false;

			@Override
			public int read() throws IOException {
				synchronized (exchange) {
					int read = exchange.getRequestBody().read();
					finished = read == -1;
					return read;
				}
			}

			@Override
			public boolean isFinished() {
				return finished;
			}

			@Override
			public boolean isReady() {
				synchronized (exchange) {
					try {
						return exchange.getRequestBody().available() > 0;
					} catch (IOException ex) {
						return false;
					}
				}
			}

			@Override
			public void setReadListener(ReadListener rl) {
				throw new UnsupportedOperationException("setReadListener is not supported!");
			}
		};
	}

	@Override
	public String getParameter(String key) {
		String[] val = params.get(key);
		return (val != null && val.length == 1) ? val[0] : null; //(val instanceof String) ? ((String) val) : null;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return new IteratorEnumerator(params.keySet().iterator());
	}

	@Override
	public String[] getParameterValues(String key) {
//		Stringp[] val = params.get(key);
//		if (val == null)
//			return null;
//		return (val instanceof String[]) ? ((String[]) val) : new String[] { (String) val };
		return params.get(key);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return Collections.unmodifiableMap(params);
	}

	@Override
	public String getProtocol() {
		synchronized (exchange) {
			return exchange.getProtocol();
		}
	}

	@Override
	public String getScheme() {
		synchronized (exchange) {
			return exchange.getProtocol();
		}
	}

	@Override
	public String getServerName() {
		synchronized (exchange) {
			return exchange.getRequestHeaders().getFirst("Host");
		}
	}

	@Override
	public int getServerPort() {
		synchronized (exchange) {
			return exchange.getLocalAddress().getPort();
		}
	}

	@Override
	public BufferedReader getReader() throws IOException {
		synchronized (exchange) {
			if (reader == null) {
				reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), characterEncoding));
			}
		}
		return reader;
	}

	@Override
	public String getRemoteAddr() {
		synchronized (exchange) {
			return exchange.getRemoteAddress().toString();
		}
	}

	@Override
	public String getRemoteHost() {
		synchronized (exchange) {
			return exchange.getRemoteAddress().getHostName();
		}
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
		synchronized (exchange) {
			return exchange.getHttpContext().getServer() instanceof HttpsServer;
		}
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
		synchronized (exchange) {
			return exchange.getRemoteAddress().getPort();
		}
	}

	@Override
	public String getLocalName() {
		synchronized (exchange) {
			return exchange.getRequestURI().getHost();
		}
	}

	@Override
	public String getLocalAddr() {
		synchronized (exchange) {
			return exchange.getLocalAddress().toString();
		}
	}

	@Override
	public int getLocalPort() {
		synchronized (exchange) {
			return exchange.getLocalAddress().getPort();
		}
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return startAsync(this, response);
	}

	@Override
	public AsyncContext startAsync(ServletRequest sr, ServletResponse sr1) throws IllegalStateException {
		async = new AsyncContextImpl(sr, sr1, exchange, timer);
		async.setTimeout(executionTimeout * 2);
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
		return cookies;
	}

	@Override
	public long getDateHeader(String string) {
		return 0;
	}

	@Override
	public String getHeader(String string) {
		synchronized (exchange) {
			return exchange.getRequestHeaders().getFirst(string);
		}
	}

	@Override
	public Enumeration<String> getHeaders(String string) {
		return null;
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		synchronized (exchange) {
			return Collections.enumeration(exchange.getRequestHeaders().keySet());
		}
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
		int start = contextPath.length() + servletPath.length();
		if (servletPath.endsWith("/")) {
			start -= 1;
		}
		return getRequestURI().substring(start);
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
		synchronized (exchange) {
			return exchange.getRequestURI().getQuery();
		}
	}

	@Override
	public String getRemoteUser() {
		return null;
	}

	@Override
	public boolean isUserInRole(String role) {
		Principal principal = getUserPrincipal();
		if ("user".equals(role)) {
			return principal != null;
		}
		try {
			if ("admin".equals(role)) {
				return principal != null && authProvider.isAdmin(BareJID.bareJIDInstance(principal.getName()));
			}
		} catch (Exception ex) {
		}
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
					if (authProvider.checkCredentials(user, pass)) {
						final String jid = user;
						principal = new Principal() {
							@Override
							public String getName() {
								return jid;
							}
						};
					}
				} catch (TigaseStringprepException | TigaseDBException | AuthorizationException ex) {
					Logger.getLogger(DummyServletRequest.class.getName())
							.log(Level.FINEST, "could not authorize user", ex);
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
		synchronized (exchange) {
			String uri = exchange.getRequestURI().toString();
			int idx = uri.indexOf("?");
			if (idx > -1) {
				uri = uri.substring(0, idx);
			}
			return uri;
		}
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer buf = new StringBuffer();
		try {
			synchronized (exchange) {
				if (exchange.getRequestURI().isAbsolute()) {
					buf.append(exchange.getRequestURI().toURL().toExternalForm());
				} else {
					if (isSecure()) {
						buf.append("https");
					} else {
						buf.append("http");
					}
					buf.append("://").append(getServerName());
					buf.append(exchange.getRequestURI().getPath());
				}
			}
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
		synchronized (exchange) {
			if (!"HEAD".equals(exchange.getRequestMethod())) {
				exchange.getResponseBody().flush();
				exchange.getResponseBody().close();
			}
		}
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
		throw new UnsupportedOperationException(
				"Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> type) throws IOException, ServletException {
		throw new UnsupportedOperationException(
				"Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public long getContentLengthLong() {
		synchronized (exchange) {
			String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
			if (contentLength == null || contentLength.isEmpty()) {
				return 0;
			}
			return Long.parseLong(contentLength);
		}
	}

	@Override
	public String toString() {
		return "" + requestId + " for " + getRequestURI();
	}

	private void decodeParamsFromString(String query, Map<String, String[]> params) {
		for (String part : query.split("&")) {
			String[] val = part.split("=");
			try {
				String k = URLDecoder.decode(val[0], "UTF-8");
				String v = val.length == 1 ? "" : URLDecoder.decode(val[1], "UTF-8");
				if (params.containsKey(k)) {
//					if (params.get(k) instanceof String[]) {
					String[] oldV = params.get(k);
					oldV = Arrays.copyOf(oldV, oldV.length + 1);
					oldV[oldV.length - 1] = v;
					params.put(k, oldV);
//					} else {
//						params.put(k, new String[] { (String)params.get(k), v });
//					}
				} else {
					params.put(k, new String[]{v});
				}
			} catch (UnsupportedEncodingException ex) {
				Logger.getLogger(DummyServletRequest.class.getName())
						.log(Level.FINE, "could not decode URLEncoded paramters", ex);
			}
		}
	}

	private class IteratorEnumerator<T, K extends Iterator<T>>
			implements Enumeration<T> {

		private final Iterator<T> iter;

		public IteratorEnumerator(Iterator<T> iter) {
			this.iter = iter;
		}

		@Override
		public boolean hasMoreElements() {
			return iter.hasNext();
		}

		@Override
		public T nextElement() {
			return iter.next();
		}

	}
}
