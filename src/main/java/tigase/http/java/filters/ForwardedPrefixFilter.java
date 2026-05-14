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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.logging.Logger;

public class ForwardedPrefixFilter implements Filter {

	private static final Logger LOGGER = Logger.getLogger(ForwardedPrefixFilter.class.getName());

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {

		if (servletRequest instanceof HttpServletRequest request) {
            String xForwardedPrefix = request.getHeader("X-Forwarded-Prefix");
			LOGGER.finest(() -> "processing request: " + request + ", xForwardedPrefix: `" + xForwardedPrefix + "`");
			if (xForwardedPrefix != null && !xForwardedPrefix.isEmpty()) {
				filterChain.doFilter(new PrefixedContextPathRequest(request, xForwardedPrefix), servletResponse);
				return;
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {

	}

	public static class PrefixedContextPathRequest extends HttpServletRequestWrapper {

		private final String contextPath;
		private final String originalContextPath;

		PrefixedContextPathRequest(HttpServletRequest request, String prefix) {
			super(request);
			originalContextPath = request.getContextPath();
			this.contextPath = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
			LOGGER.finest(() -> "prefixedPath, originalContextPath: `" + originalContextPath + "`; contextPath: `" + contextPath + "`, request.contextPath(): `" + request.getContextPath() + "`");
		}

		@Override
		public String getContextPath() {
			return contextPath;
		}

		public String getOriginalContextPath() {
			return originalContextPath;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("PrefixedContextPathRequest{");
			sb.append("contextPath='").append(getContextPath()).append('\'');
			sb.append(", originalContextPath='").append(getOriginalContextPath()).append('\'');
			sb.append(", request=").append(getRequest());
			sb.append('}');
			return sb.toString();
		}
	}
}
