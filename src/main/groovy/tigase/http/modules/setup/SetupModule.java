/*
 * SetupModule.java
 *
 * Tigase HTTP API
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

import tigase.auth.credentials.Credentials;
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.api.Service;
import tigase.http.modules.AbstractBareModule;
import tigase.http.modules.Module;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import java.util.logging.Logger;

/**
 * @author andrzej
 */
@Bean(name = "setup", parent = HttpMessageReceiver.class, active = true)
public class SetupModule
		extends AbstractBareModule
		implements Module {

	private static final Logger log = Logger.getLogger(SetupModule.class.getCanonicalName());
	@ConfigField(desc = "Allow access to setup with password", alias = "admin-password")
	private String adminPassword = null;
	@ConfigField(desc = "Allow access to setup for user", alias = "admin-user")
	private String adminUser = null;
	@Inject(nullAllowed = true)
	private AuthRepository authRepo;
	private DeploymentInfo httpDeployment = null;
	private Service service = null;

	@Override
	public String getDescription() {
		return "Setup - handles basic configuration of Tigase XMPP Server";
	}

	@Override
	public boolean isRequestAllowed(String key, String domain, String path) {
		return false;
	}

	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}

		service = new tigase.http.ServiceImpl(this) {

			@Override
			public boolean isAdmin(BareJID user) {
				return user.toString().equals(adminUser) || super.isAdmin(user);
			}

			@Override
			public boolean checkCredentials(String user, String password)
					throws TigaseStringprepException, TigaseDBException, AuthorizationException {
				if (adminUser != null && adminPassword != null && adminUser.equals(user) &&
						adminPassword.equals(password)) {
					return true;
				}

				AuthRepository authRepository = SetupModule.this.getAuthRepository();
				if (authRepository == null) {
					return false;
				}
				BareJID jid = BareJID.bareJIDInstance(user);
				Credentials credentials = authRepository.
						getCredentials(jid, Credentials.DEFAULT_USERNAME);
				if (credentials == null) {
					return false;
				}
				return credentials.getFirst().verifyPlainPassword(password);
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
		ServletInfo servletInfo = httpServer.servlet("SetupServlet", SetupServlet.class).addInitParam("module", uuid);
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

	@Override
	public UserRepository getUserRepository() {
		return null;
	}

	@Override
	public AuthRepository getAuthRepository() {
		return authRepo;
	}

	protected Service getService() {
		return service;
	}
}
