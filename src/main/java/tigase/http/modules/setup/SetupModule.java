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
package tigase.http.modules.setup;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.http.AuthProvider;
import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.jaxrs.AbstractJaxRsModule;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.util.AssetsServlet;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import javax.naming.AuthenticationException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@Bean(name = "setup", parent = HttpMessageReceiver.class, active = true)
public class SetupModule extends AbstractJaxRsModule<SetupHandler> {

	private static final Logger log = Logger.getLogger(SetupModule.class.getCanonicalName());
	@ConfigField(desc = "Allow access to setup with password", alias = "admin-password")
	private String adminPassword = null;
	@ConfigField(desc = "Allow access to setup for user", alias = "admin-user")
	private String adminUser = null;

	private Config config = new Config();

	private ScheduledExecutorService executorService;

	@Inject(nullAllowed = true)
	private List<SetupHandler> handlersAll;
	private List<SetupHandler> handlers = new ArrayList<>();
	@Inject(nullAllowed = true)
	private AuthRepository authRepository;

	private DeploymentInfo httpDeployment;

	public Config getConfig() {
		return config;
	}

	@Override
	public String getDescription() {
		return "Setup";
	}
	
	public void setHandlersAll(List<SetupHandler> handlers) {
		if (handlers == null) {
			this.handlersAll = new ArrayList<>();
		} else {
			this.handlersAll = handlers;
		}

		Optional<SetupHandler> handlerOptional = handlersAll.stream()
				.filter(h -> h.getClass().getAnnotation(InitialPage.class) != null)
				.findFirst();

		if (handlerOptional.isPresent()) {
			List<SetupHandler> value = new ArrayList<>();
			SetupHandler handler = handlerOptional.get();
			value.add(handler);
			NextPage nextPage;
			while ((nextPage = handler.getClass().getAnnotation(NextPage.class)) != null) {
				handler = null;
				for (SetupHandler it : handlers) {
					if (nextPage.value().isInstance(it)) {
						handler = it;
					}
				}
				if (handler == null) {
					break;
				}

				value.add(handler);
			}
			this.handlers = value;
		} else {
			this.handlers = Collections.EMPTY_LIST;
		}

	}

	public ScheduledExecutorService getExecutorService() {
		return executorService;
	}

	public List<SetupHandler> getHandlers() {
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
				.setAuthProvider(new AuthProvider() {
					@Override
					public boolean isAdmin(BareJID user) {
						return Optional.ofNullable(user)
								.map(BareJID::toString)
								.filter(userStr -> Objects.equals(userStr, adminUser))
								.isPresent() || getAuthProvider().isAdmin(user);
					}

					@Override
					public boolean checkCredentials(String user, String password)
							throws TigaseStringprepException, TigaseDBException {
						if (Objects.equals(user, adminUser) && Objects.equals(password, adminPassword)) {
							return true;
						}
						return getAuthProvider().checkCredentials(user, password);
					}

					@Override
					public String generateToken(JWTPayload token) throws NoSuchAlgorithmException, InvalidKeyException {
						throw new RuntimeException("Feature not implemented!");
					}

					@Override
					public JWTPayload parseToken(String token) throws AuthenticationException {
						throw new RuntimeException("Feature not implemented!");
					}

					@Override
					public JWTPayload authenticateWithCookie(HttpServletRequest request) {
						return null;
					}

					@Override
					public void setAuthenticationCookie(HttpServletResponse response, JWTPayload payload, String domain,
														String path)
							throws NoSuchAlgorithmException, InvalidKeyException {
						throw new RuntimeException("Feature not implemented!");
					}

					@Override
					public void resetAuthenticationCookie(HttpServletResponse response, String domain, String path) {
						throw new RuntimeException("Feature not implemented!");
					}

					@Override
					public void refreshJwtToken(HttpServletRequest request, HttpServletResponse response) {
						// nothing to do...
					}
				})
				.setDeploymentName("Setup")
				.setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}

		ServletInfo servletInfo = httpServer.servlet("SetupServlet", SetupServlet.class);
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

}