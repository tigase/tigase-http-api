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
package tigase.http.modules;

import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.java.filters.ForwardedPrefixFilter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "redirect", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
             ConfigTypeEnum.ComponentMode})
public class RedirectModule extends AbstractModule {

	@ConfigField(desc = "Redirect to path")
	private String redirectTo = "/dashboard/";
	
	public RedirectModule() {
		contextPath = "/";
	}

	private DeploymentInfo httpDeployment = null;

	public void setName(String name) {
		this.name = name;
		contextPath = "/";
	}

	@Override
	public String getDescription() {
		return "Redirect to the specified module";
	}

	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}

		httpDeployment = httpServer
				.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath)
				.setDeploymentName("Redirect")
				.setDeploymentDescription(getDescription());

		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}


		ServletInfo servletInfo = httpServer.servlet("RedirectServet", RedirectServlet.class);
		servletInfo.addInitParam("redirectTo", redirectTo);
		servletInfo.addMapping("/");
		httpDeployment.addServlets(servletInfo);

		httpServer.deploy(httpDeployment);
	}

	@Override
	public void stop() {
		if (httpDeployment != null) {
			httpServer.undeploy(httpDeployment);
			httpDeployment = null;
		}
		super.stop();
	}

	public static class RedirectServlet extends HttpServlet {

		private static final Logger log = Logger.getLogger(RedirectServlet.class.getCanonicalName());

		private String redirectTo;

		public RedirectServlet() {}

		@Override
		public void init() throws ServletException {
			super.init();
			ServletConfig config = super.getServletConfig();
			redirectTo = config.getInitParameter("redirectTo");
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			String requestUri = req.getRequestURI();
			String contextPath = req.getContextPath();
			if (req instanceof ForwardedPrefixFilter.PrefixedContextPathRequest forwardedRequest) {
				contextPath = forwardedRequest.getOriginalContextPath();
			}

			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
				        "Handling request: " + req.getMethod() + " :: " + requestUri + "; contextPath: " + contextPath +
						        ", request.contextPath" + req.getContextPath() + ", request: " + req);
			}
			if (!contextPath.equals("/")) {
				if (!contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
					requestUri = requestUri.substring(contextPath.length());
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
						        "Setting request URI from: " + req.getRequestURI() + " to: " + requestUri);
					}
				}
			}

			String url = requestUri.substring(1);
			String redirectTo = this.redirectTo + url;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Sending redirect to " + redirectTo);
			}

			resp.sendRedirect(redirectTo);
		}
	}
}
