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

import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.TimeoutHandler;
import tigase.http.api.HttpException;
import tigase.http.jaxrs.RequestHandler;
import tigase.http.jaxrs.utils.JaxRsUtil;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AsyncResponseImpl implements AsyncResponse {

	private static final Logger log = Logger.getLogger(AsyncResponseImpl.class.getCanonicalName());

	private final Optional<String> acceptedType;
	private final ScheduledExecutorService executorService;
	private final RequestHandler requestHandler;
	private Future timeoutFuture;
	private TimeoutHandler timeoutHandler;
	private final AsyncContext context;
	private State state = State.suspended;

	public AsyncResponseImpl(RequestHandler requestHandler, ScheduledExecutorService executorService, HttpServletRequest request, Optional<String> acceptedType) {
		this.requestHandler = requestHandler;
		this.executorService = executorService;
		this.context = request.startAsync();
		this.acceptedType = acceptedType;
	}

	@Override
	public boolean resume(Object o) {
		if (timeoutFuture != null) {
			timeoutFuture.cancel(false);
		}
		try {
			requestHandler.sendEncodedContent(o, acceptedType, (HttpServletResponse) context.getResponse());
		} catch (Throwable ex) {
			sendError(ex);
		}
		context.complete();
		state = State.done;
		return true;
	}

	@Override
	public boolean resume(Throwable ex) {
		if (timeoutFuture != null) {
			timeoutFuture.cancel(false);
		}
		sendError(ex);
		context.complete();
		state = State.done;
		return true;
	}

	private void sendError(Throwable ex) {
		try {
			HttpException he = JaxRsUtil.convertExceptionForResponse(ex);
			((HttpServletResponse) context.getResponse()).sendError(he.getCode(), he.getMessage());
		} catch (IOException ex1) {
			log.log(Level.FINEST, "Failed to send error message", ex1);
		}
	}

	@Override
	public boolean cancel() {
		try {
			if (timeoutFuture != null) {
				timeoutFuture.cancel(false);
			}
			((HttpServletResponse) context.getResponse()).sendError(503, "Service unavailable");
		} catch (IOException ex) {
			log.log(Level.FINEST, "Failed to send error message", ex);
		}
		context.complete();
		state = State.cancelled;
		return true;
	}

	@Override
	public boolean cancel(int i) {
		return cancel();
	}

	@Override
	public boolean cancel(Date date) {
		return cancel();
	}

	@Override
	public boolean isSuspended() {
		return state == State.suspended;
	}

	@Override
	public boolean isCancelled() {
		return state == State.cancelled;
	}

	@Override
	public boolean isDone() {
		return state == State.done;
	}
	
	@Override
	public boolean setTimeout(long delay, TimeUnit timeUnit) {
		if (timeoutFuture != null) {
			timeoutFuture.cancel(false);
		}
		timeoutFuture =  executorService.schedule(new Runnable() {
			@Override
			public void run() {
				if (timeoutHandler != null) {
					try {
						timeoutHandler.handleTimeout(AsyncResponseImpl.this);
					} catch (Throwable ex) {
						cancel();
					}
				}
			}
		}, delay, timeUnit);
		return true;
	}

	@Override
	public void setTimeoutHandler(TimeoutHandler timeoutHandler) {
		this.timeoutHandler = timeoutHandler;
	}

	@Override
	public Collection<Class<?>> register(Class<?> aClass) {
		throw new UnsupportedOperationException("Feature not implemented!");
	}

	@Override
	public Map<Class<?>, Collection<Class<?>>> register(Class<?> aClass, Class<?>... classes) {
		throw new UnsupportedOperationException("Feature not implemented!");
	}

	@Override
	public Collection<Class<?>> register(Object o) {
		throw new UnsupportedOperationException("Feature not implemented!");
	}

	@Override
	public Map<Class<?>, Collection<Class<?>>> register(Object o, Object... objects) {
		throw new UnsupportedOperationException("Feature not implemented!");
	}

	public enum State {
		suspended,
		done,
		cancelled
	}
}
