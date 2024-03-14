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

import tigase.http.AuthProvider;
import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.jaxrs.Handler;
import tigase.http.jaxrs.JaxRsModule;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.modules.AbstractModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "rest", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class RestModule extends AbstractModule
		implements JaxRsModule {

	private static final Logger log = Logger.getLogger(RestModule.class.getCanonicalName());
	private static final String DEF_SCRIPTS_DIR_VAL = "scripts/rest";
	private static final String SCRIPTS_DIR_KEY = "rest-scripts-dir";

	private ScheduledExecutorService executorService;

	@Inject
	private ApiKeyRepository apiKeyRepository;
	@Inject
	private AuthProvider authProvider;

	@Inject(nullAllowed = true)
	private List<Handler> handlers;

	private DeploymentInfo httpDeployment;

	@ConfigField(desc = "Scripts directory", alias = SCRIPTS_DIR_KEY)
	private String scriptsDir = DEF_SCRIPTS_DIR_VAL;

	@Override
	public String getDescription() {
		return "REST support - handles HTTP REST access using scripts";
	}

	public void setHandlers(List<Handler> handlers) {
		if (handlers == null) {
			this.handlers = new ArrayList<>();
		} else {
			this.handlers = handlers;
		}
	}

	public static File[] getGroovyFiles(File scriptsDirFile) {
		if (scriptsDirFile.exists()) {
			return scriptsDirFile.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File file, String s) {
					return s.endsWith("groovy");
				}
			});
		} else {
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, "scripts directory {0} does not exist!", scriptsDirFile);
			}
			return new File[0];
		}
	}

	public ApiKeyRepository getApiKeyRepository() {
		return apiKeyRepository;
	}

	public ScheduledExecutorService getExecutorService() {
		return executorService;
	}

	public List<Handler> getHandlers() {
		return handlers;
	}

	public Kernel getKernel() {
		return getKernel(uuid);
	}
	
	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}

		if (executorService != null) {
		executorService.shutdown();
		}
		executorService = Executors.newSingleThreadScheduledExecutor();

		super.start();
		httpDeployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath)
				.setAuthProvider(authProvider)
				.setDeploymentName("HTTP/REST API")
				.setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}

		try {
			File scriptsDirFile = new File(scriptsDir);
			ServletInfo servletInfo = httpServer.servlet("RestServlet", RestServlet.class);
			servletInfo.addInitParam(JaxRsServlet.MODULE_KEY, uuid)
					.addInitParam(RestServlet.SCRIPTS_DIR_KEY, scriptsDirFile.getCanonicalPath())
					.addMapping("/*");

			httpDeployment.addServlets(servletInfo);
		} catch (IOException ex) {
			log.log(Level.FINE, "Exception while scanning for scripts to load", ex);
		}
		httpServer.deploy(httpDeployment);
	}

	@Override
	public void stop() {
		if (httpDeployment != null) {
			httpServer.undeploy(httpDeployment);
			httpDeployment = null;
		}
		if (executorService != null) {
			executorService.shutdown();
		}
		super.stop();
	}

	public boolean isRequestAllowed(String key, String domain, String path) {
		return apiKeyRepository.isAllowed(key, domain, path);
	}
}
