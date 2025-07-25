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
package tigase.http.jetty;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import tigase.http.AuthProvider;
import tigase.http.DeploymentInfo;
import tigase.http.ServletInfo;
import tigase.http.api.HttpServerIfc;
import tigase.http.java.filters.ProtocolRedirectFilter;
import tigase.http.jetty.security.BasicAndJWTAuthenticator;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 06.08.2016.
 */
public class JettyHttpServerHelper {

	public static final String CONTEXT_KEY = "context-key";

	private static final Logger log = Logger.getLogger(JettyHttpServerHelper.class.getCanonicalName());

	public static ServletContextHandler createServletContextHandler(DeploymentInfo deployment, HttpServerIfc server) {

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		try {
			context.setSecurityHandler(context.getDefaultSecurityHandlerClass().newInstance());
		} catch (InstantiationException ex) {
			log.log(Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			log.log(Level.SEVERE, null, ex);
		}
		AuthProvider authProvider = deployment.getAuthProvider();
		if (authProvider != null) {
			context.getSecurityHandler().setAuthenticator(new BasicAndJWTAuthenticator());
			context.getSecurityHandler()
					.setLoginService(new tigase.http.jetty.security.TigasePlainLoginService(authProvider));
		}
		context.setContextPath(deployment.getContextPath());
		if (deployment.getClassLoader() != null) {
			context.setClassLoader(deployment.getClassLoader());
		}
		String[] vhosts = deployment.getVHosts();
		if (vhosts != null && vhosts.length > 0) {
			context.setVirtualHosts(vhosts);
		}
		ServletInfo[] servletInfos = deployment.getServlets();
		for (ServletInfo info : servletInfos) {
			for (String mapping : info.getMappings()) {
				ServletHolder holder = new ServletHolder(mapping, info.getServletClass());
				holder.setInitParameters(info.getInitParams());
				context.addServlet(holder, mapping);
			}
		}

		Map<String, String> filterParams = new HashMap<>();
		filterParams.put("serverBeanName", server.getName());
		context.addFilter(ProtocolRedirectFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST)).setInitParameters(Collections.unmodifiableMap(filterParams));

		Map<Class<? extends Throwable>, String> exceptionErrorPages = deployment.getExceptionErrorPages();
		Map<Integer,String> errorCodePages = deployment.getErrorCodePages();
		if (!(exceptionErrorPages.isEmpty() && errorCodePages.isEmpty() && deployment.getGlobalErrorPage() == null)) {
			ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler() {
				@Override
				public String getErrorPage(HttpServletRequest request) {
					String accept = request.getHeader(HttpHeader.ACCEPT.asString());
					// if accept doesn't contains text/html return null so we could generate a generic json/xml error
					if (accept == null || accept.contains("text/html")) {
						return super.getErrorPage(request);
					}
					return null;
				}
			};
			exceptionErrorPages.forEach(errorHandler::addErrorPage);
			errorCodePages.forEach(errorHandler::addErrorPage);
			Optional.ofNullable(deployment.getGlobalErrorPage()).ifPresent(uri ->errorHandler.addErrorPage("org.eclipse.jetty.server.error_page.global", uri));
			context.setErrorHandler(errorHandler);
		}

		return context;
	}

}
