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
package tigase.http.java;

import com.sun.net.httpserver.HttpExchange;

import javax.servlet.*;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author andrzej
 */
public class AsyncContextImpl
		implements AsyncContext {

	private static final Logger log = Logger.getLogger(AsyncContextImpl.class.getCanonicalName());
	private final HttpExchange exchange;
	private final ServletRequest req;
	private final ServletResponse resp;
	private final ScheduledExecutorService scheduledExecutor;
	private ScheduledFuture future;
	private long timeout;

	public AsyncContextImpl(ServletRequest req, ServletResponse resp, HttpExchange exchange, ScheduledExecutorService scheduledExecutor) {
		this.req = req;
		this.resp = resp;
		this.exchange = exchange;
		this.scheduledExecutor = scheduledExecutor;
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
		cancel();
		try {
			resp.flushBuffer();
		} catch (IOException ex) {
			log.log(Level.FINEST, "Failure during completion of async task", ex);
		}
		exchange.close();
	}

	@Override
	public void start(Runnable r) {
	}

	@Override
	public void addListener(AsyncListener al) {
		throw new UnsupportedOperationException(
				"Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addListener(AsyncListener al, ServletRequest sr, ServletResponse sr1) {
		throw new UnsupportedOperationException(
				"Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T extends AsyncListener> T createListener(Class<T> type) throws ServletException {
		throw new UnsupportedOperationException(
				"Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(long timeout) {
		this.timeout = timeout;
		this.future = scheduledExecutor.schedule(this::timeout, timeout, TimeUnit.MILLISECONDS);
	}

	private void timeout() {
		try {
			exchange.sendResponseHeaders(504, -1);
		} catch (IOException ex) {
			log.log(Level.FINEST, " failed to send 504 error", ex);
		}
		exchange.close();
	}

	public void cancel() {
		if (future != null) {
			future.cancel(false);
		}
	}
}
