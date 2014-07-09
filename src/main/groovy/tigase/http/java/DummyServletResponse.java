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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author andrzej
 */
public class DummyServletResponse implements HttpServletResponse {

	private final HttpExchange exchange;
	private PrintWriter writer;
	
	public DummyServletResponse(HttpExchange exchange) {
		this.exchange = exchange;
	}
	
	@Override
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				if (exchange.getResponseCode() == -1)
					exchange.sendResponseHeaders(200, 0);
				exchange.getResponseBody().write(b);
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(WriteListener wl) {
			}
		};
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			if (exchange.getResponseCode() == -1)
					exchange.sendResponseHeaders(200, 0);
			writer =  new PrintWriter(exchange.getResponseBody());	
		}
		return writer;
	}

	@Override
	public void setCharacterEncoding(String string) {
	}

	@Override
	public void setContentLength(int i) {
		exchange.getResponseHeaders().set("Content-Length", String.valueOf(i));
	}

	@Override
	public void setContentType(String string) {
		exchange.getResponseHeaders().set("Content-Type", string);
	}

	@Override
	public void setBufferSize(int i) {
	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public void flushBuffer() throws IOException {
		if (writer != null) {
			writer.flush();
		}
		exchange.getResponseBody().flush();
	}

	@Override
	public void resetBuffer() {
	}

	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void reset() {
	}

	@Override
	public void setLocale(Locale locale) {
	}

	@Override
	public Locale getLocale() {
		return null;
	}

	@Override
	public void addCookie(Cookie cookie) {
	}

	@Override
	public boolean containsHeader(String string) {
		return false;
	}

	@Override
	public String encodeURL(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(DummyServletResponse.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	@Override
	public String encodeRedirectURL(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(DummyServletResponse.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	@Override
	public String encodeUrl(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(DummyServletResponse.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	@Override
	public String encodeRedirectUrl(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(DummyServletResponse.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	@Override
	public void sendError(int i, String string) throws IOException {
		if (string != null) {
			exchange.sendResponseHeaders(i, string.getBytes().length);
			PrintWriter writer = getWriter();
			writer.write(string);
			writer.flush();
		}
		else {
			exchange.sendResponseHeaders(i, 0);
		}
	}

	@Override
	public void sendError(int i) throws IOException {
		exchange.sendResponseHeaders(i, 0);
	}

	@Override
	public void sendRedirect(String string) throws IOException {
		exchange.sendResponseHeaders(302, 0);
	}

	@Override
	public void setDateHeader(String string, long l) {
	}

	@Override
	public void addDateHeader(String string, long l) {
	}

	@Override
	public void setHeader(String string, String string1) {
		exchange.getResponseHeaders().set(string, string1);
	}

	@Override
	public void addHeader(String string, String string1) {
		exchange.getResponseHeaders().add(string, string1);
	}

	@Override
	public void setIntHeader(String string, int i) {
	}

	@Override
	public void addIntHeader(String string, int i) {
	}

	@Override
	public void setStatus(int i) {
		try {
			exchange.sendResponseHeaders(i, 0);
		} catch (IOException ex) {
			Logger.getLogger(DummyServletResponse.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void setStatus(int i, String string) {
		try {
			exchange.sendResponseHeaders(i, 0);
		} catch (IOException ex) {
			Logger.getLogger(DummyServletResponse.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public int getStatus() {
		return 200;
	}

	@Override
	public String getHeader(String string) {
		return exchange.getResponseHeaders().getFirst(string);
	}

	@Override
	public Collection<String> getHeaders(String string) {
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		return null;
	}

	@Override
	public void setContentLengthLong(long l) {
		setHeader("Content-Length", String.valueOf(l));
	}
	
}