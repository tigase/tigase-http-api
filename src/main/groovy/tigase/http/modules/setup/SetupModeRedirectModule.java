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
package tigase.http.modules.setup;

import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServiceImpl;
import tigase.http.ServletInfo;
import tigase.http.modules.AbstractBareModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

/**
 * Created by andrzej on 06.04.2017.
 */
@Bean(name = "setupRedirect", parent = HttpMessageReceiver.class, active = true)
@ConfigType(ConfigTypeEnum.SetupMode)
public class SetupModeRedirectModule
		extends AbstractBareModule {

	private DeploymentInfo httpDeployment;
	private ServiceImpl service;

	public SetupModeRedirectModule() {
		contextPath = "/";
	}

	public void setName(String name) {
		this.name = name;
		contextPath = "/";
	}

	@Override
	public String getDescription() {
		return "Setup mode redirection module";
	}

	@Override
	public boolean isRequestAllowed(String key, String domain, String path) {
		return true;
	}

	@Override
	public UserRepository getUserRepository() {
		return null;
	}

	@Override
	public AuthRepository getAuthRepository() {
		return null;
	}

	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}

		service = new tigase.http.ServiceImpl(this) {

			@Override
			public boolean isAdmin(BareJID user) {
				return true;
			}

			@Override
			public boolean checkCredentials(String user, String password)
					throws TigaseStringprepException, TigaseDBException, AuthorizationException {
				return true;
			}

		};

		super.start();

		httpDeployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath)
				.setService(service)
				.setDeploymentName("Setup")
				.setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}
		ServletInfo servletInfo = httpServer.servlet("SetupModeRedirectServlet", SetupModeRedirectServlet.class)
				.addInitParam("module", uuid);
		servletInfo.addMapping("/*");
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
}
