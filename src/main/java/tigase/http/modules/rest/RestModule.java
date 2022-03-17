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

import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.PacketWriter;
import tigase.http.ServletInfo;
import tigase.http.jaxrs.JaxRsModule;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.modules.AbstractModule;
import tigase.http.util.AssetsServlet;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.server.script.CommandIfc;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Bean(name = "rest", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class RestModule extends AbstractModule
		implements JaxRsModule<RestHandler> {

	private ScheduledExecutorService executorService;

	@Inject
	private ApiKeyRepository apiKeyRepository;

	@Inject(nullAllowed = true)
	private List<RestHandler> handlers;

	private DeploymentInfo httpDeployment;

	private final CommandIfc[] commands = new CommandIfc[]{new ApiKeyAddCmd(this), new ApiKeyRemoveCmd(this),
														   new ApiKeyUpdateCmd(this)};

	@Override
	public String getDescription() {
		return "REST support - handles HTTP REST access using scripts";
	}

	public void setHandlers(List<RestHandler> handlers) {
		if (handlers == null) {
			this.handlers = new ArrayList<>();
		} else {
			this.handlers = handlers;
		}
	}

	public ApiKeyRepository getApiKeyRepository() {
		return apiKeyRepository;
	}

	public void setApiKeyRepository(ApiKeyRepository apiKeyRepository) {
		if (getComponentName() != null) {
			apiKeyRepository.setRepoUser(BareJID.bareJIDInstanceNS(getName(), getComponentName()));
			apiKeyRepository.setRepo(getUserRepository());
		}
		this.apiKeyRepository = apiKeyRepository;
	}

	public ScheduledExecutorService getExecutorService() {
		return executorService;
	}

	public List<RestHandler> getHandlers() {
		return handlers;
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
				.setDeploymentName("HTTP/REST API")
				.setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}

		ServletInfo servletInfo = httpServer.servlet("RestServlet", RestServlet.class);
		servletInfo.addInitParam(JaxRsServlet.MODULE_KEY, uuid).addMapping("/*");
		httpDeployment.addServlets(servletInfo);

		servletInfo = httpServer.servlet("AssetsServlet", AssetsServlet.class);
		servletInfo.addMapping("/assets/*");
		httpDeployment.addServlets(servletInfo);

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
		apiKeyRepository.reload();
		ApiKeyItem item = apiKeyRepository.getItem(key);
		if (item == null) {
			return false;
		}
		return item.isAllowed(key, domain,path);
	}

	@Override
	public void init(JID jid, String componentName, PacketWriter writer) {
		super.init(jid, componentName, writer);
		setApiKeyRepository(apiKeyRepository);
	}

	@Override
	public void initialize() {
		super.initialize();
		Arrays.stream(commands).forEach(commandManager::registerCmd);
	}

	@Override
	public void beforeUnregister() {
		super.beforeUnregister();
		Arrays.stream(commands).forEach(commandManager::unregisterCmd);
	}
}
