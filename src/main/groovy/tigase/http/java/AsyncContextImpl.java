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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 * @author andrzej
 */
public class AsyncContextImpl implements AsyncContext {

	private final ServletRequest req;
	private final ServletResponse resp;
	private final HttpExchange exchange;
	
	public AsyncContextImpl(ServletRequest req, ServletResponse resp, HttpExchange exchange) {
		this.req = req;
		this.resp = resp;
		this.exchange = exchange;
	}
	
	@Override
	public ServletRequest getRequest() {
		return req;
	}

	@Override
	public ServletResponse getResponse() {
		return resp;
	}

	@Override
	public boolean hasOriginalRequestAndResponse() {
		return true;
	}

	@Override
	public void dispatch() {
	}

	@Override
	public void dispatch(String string) {
	}

	@Override
	public void dispatch(ServletContext sc, String string) {
	}

	@Override
	public void complete() {
		try {
			resp.flushBuffer();
			exchange.getResponseBody().close();
		} catch (IOException ex) {
			Logger.getLogger(AsyncContextImpl.class.getName()).log(Level.FINE, null, ex);
		}
	}

	@Override
	public void start(Runnable r) {
	}

	@Override
	public void addListener(AsyncListener al) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addListener(AsyncListener al, ServletRequest sr, ServletResponse sr1) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T extends AsyncListener> T createListener(Class<T> type) throws ServletException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setTimeout(long l) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public long getTimeout() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
