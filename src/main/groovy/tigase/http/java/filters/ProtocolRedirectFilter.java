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

import tigase.http.AbstractHttpServer;
import tigase.net.SocketType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProtocolRedirectFilter
		implements Filter {

	private static final Logger log = Logger.getLogger(ProtocolRedirectFilter.class.getCanonicalName());

	private String serverBeanName;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		serverBeanName = filterConfig.getInitParameter("serverBeanName");
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
			final HttpServletRequest request = (HttpServletRequest) servletRequest;
			final HttpServletResponse response = (HttpServletResponse) servletResponse;

			Optional<AbstractHttpServer.PortConfigBean> portConfigBean = AbstractHttpServer.getPortConfig(
					serverBeanName, request.getLocalPort());

			if (log.isLoggable(Level.FINEST)) {
				/* @formatter:off */
				log.log(Level.FINEST,
						"X-Forwarded-Proto: " + request.getHeader("X-Forwarded-Proto")
								+ ", request protocol: " + request.getProtocol()
								+ ", socket protocol: " + portConfigBean.map(AbstractHttpServer.PortConfigBean::getSocket)
								+ ", redirect URL: " + portConfigBean.map(AbstractHttpServer.PortConfigBean::getRedirectUri) +
								", condition: " + portConfigBean.map(AbstractHttpServer.PortConfigBean::getRedirectCondition));
				/* @formatter:on */
			}

			if (portConfigBean.isPresent() && shouldRedirect(request, portConfigBean.get())) {
				sendRedirect(request, response, portConfigBean.get());
				return;
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {

	}

	protected boolean shouldRedirect(HttpServletRequest request, AbstractHttpServer.PortConfigBean configBean) {
		if (configBean.getRedirectUri() == null) {
			return false;
		}

		switch (configBean.getRedirectCondition()) {
			case never:
				return false;
			case always:
				return true;
			case http:
				return protocolMatches("http", request, configBean);
			case https:
				return protocolMatches("https", request, configBean);
			default:
				throw new IllegalArgumentException("Unsupported redirect condition = " + configBean.getRedirectCondition());
		}
	}

	private boolean protocolMatches(String expectedProtocol, HttpServletRequest request, AbstractHttpServer.PortConfigBean configBean) {
		String forwardedProtocol = request.getHeader("X-Forwarded-Proto");
		if (forwardedProtocol != null) {
			return expectedProtocol.matches(forwardedProtocol);
		}

		return redirectionConditionSatisfied(configBean);
	}

	protected boolean redirectionConditionSatisfied(AbstractHttpServer.PortConfigBean configBean) {
		switch (configBean.getRedirectCondition()) {
			case http:
				return configBean.getSocket() == SocketType.plain;
			case https:
				return configBean.getSocket() != SocketType.plain;
			default:
				throw new IllegalArgumentException("Unsupported redirect condition = " + configBean.getRedirectCondition());
		}
	}

	protected void sendRedirect(HttpServletRequest request, HttpServletResponse response, AbstractHttpServer.PortConfigBean configBean)
			throws IOException {
		final String originalRequestHost = Optional.ofNullable(request.getHeader("Host"))
				.map(this::parseHostname)
				.orElse("localhost");
		final String requestQuery = Optional.ofNullable(request.getQueryString())
				.map(query -> "?" + query)
				.orElse("");
		String uri = configBean.getRedirectUri().replace("{host}", originalRequestHost) +
				request.getRequestURI() + requestQuery;
		response.sendRedirect(uri);
	}

	private String parseHostname(String host) {
		int idx = host.indexOf(":");
		return idx >= 0 ? host.substring(0, idx) : host;
	}
}
