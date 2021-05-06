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
package tigase.http.java.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class DummyFilterChain implements FilterChain {

	private final HttpServlet servlet;

	private Deque<Filter> filters = new ArrayDeque<Filter>();

	public DummyFilterChain(HttpServlet servlet) {
		this.servlet = servlet;
	}

	public DummyFilterChain addFilter(Filter filter) {
		filters.offer(filter);
		return this;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
			throws IOException, ServletException {
		Filter filter = filters.poll();
		if (filter != null) {
			filter.doFilter(servletRequest, servletResponse, this);
		} else {
			servlet.service(servletRequest, servletResponse);
		}
	}
}
