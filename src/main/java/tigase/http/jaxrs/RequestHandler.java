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
package tigase.http.jaxrs;

import tigase.http.api.HttpException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a handler for processing HTTP requests in the JAX-RS framework.
 *
 * <p>This interface defines the contract for HTTP request handlers that route requests,
 * perform authentication checks, and execute request processing logic. Implementations
 * of this interface are used by {@link JaxRsServlet} to match incoming HTTP requests
 * against registered handlers and delegate execution to the appropriate handler.</p>
 *
 * <p>Each handler is associated with:
 * <ul>
 *   <li>A specific HTTP method (GET, POST, PUT, DELETE, etc.)</li>
 *   <li>A URL pattern for request matching</li>
 *   <li>Authentication and authorization requirements</li>
 *   <li>Execution logic for processing matched requests</li>
 * </ul>
 * </p>
 *
 * <p>The interface extends {@link Comparable} to enable natural ordering of handlers,
 * which is used during request matching to ensure handlers are evaluated in the correct
 * priority order. This allows more specific patterns to be matched before general ones.</p>
 *
 * @see JaxRsServlet
 * @see Handler
 * @see HttpMethod
 */
public interface RequestHandler extends Comparable<RequestHandler> {

	public static Comparator<Pattern> PATTERN_COMPARATOR = Comparator.comparing((Pattern pattern) -> pattern.pattern().length()).reversed();
	
	Handler getHandler();

	HttpMethod getHttpMethod();

	Handler.Role getRequiredRole();

	default Set<String> getAllowedRoles() {
		return null;
	}

	default boolean isAuthenticationRequired() {
		return getRequiredRole().isAuthenticationRequired();
	}
	
	Matcher test(HttpServletRequest request, String requestUri);

	void execute(HttpServletRequest request, HttpServletResponse response, Matcher matcher,
						ScheduledExecutorService executorService) throws HttpException, IOException;
}
