/*
 * Tigase HTTP API
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.http.modules.setup;

import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.api.Service;
import tigase.http.modules.AbstractModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
@Bean(name = "setup", parent = HttpMessageReceiver.class, active = true)
public class SetupModule extends AbstractModule {

	private static final Logger log = Logger.getLogger(SetupModule.class.getCanonicalName());

	@ConfigField(desc = "Allow particular username and password to access setup page")
	private static final String CREDENTIALS_KEY = "admin-credentials";

	private DeploymentInfo httpDeployment = null;

	private Service service = null;

	private String adminUser = null;
	private String adminPassword = null;

	private final String uuid = UUID.randomUUID().toString();
	private static final ConcurrentHashMap<String,AbstractModule> modules = new ConcurrentHashMap<String,AbstractModule>();
	
	public static AbstractModule getModuleByUUID(String uuid) {
		return modules.get(uuid);
	}	

	@Override
	public String getDescription() {
		return "Setup - handles basic configuration of Tigase XMPP Server";
	}
	
	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}
	
		super.start();

		service = new tigase.http.ServiceImpl(this) {

			@Override
			public boolean isAdmin(BareJID user) {
				return user.toString().equals(adminUser) || super.isAdmin(user);
			}

			@Override
			public boolean checkCredentials(String user, String password) throws TigaseStringprepException, TigaseDBException, AuthorizationException {
				if (adminUser != null && adminPassword != null && adminUser.equals(user) && adminPassword.equals(password)) {
					return true;
				}

				AuthRepository authRepository = SetupModule.this.getAuthRepository();
				if (authRepository == null)
					return false;
				BareJID jid = BareJID.bareJIDInstance(user);
				return authRepository.plainAuth(jid, password);
			}

		};

		modules.put(uuid, this);
		httpDeployment = httpServer.deployment().setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath).setService(service).setDeploymentName("Setup").setDeploymentDescription(getDescription());
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
			modules.remove(uuid, this);
			httpDeployment = null;
		}
		super.stop();
	}	

	protected Service getService() {
		return service;
	}
}
