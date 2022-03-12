package tigase.http.modules.setup;

import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.http.AuthProvider;
import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.jaxrs.JaxRsModule;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.modules.AbstractBareModule;
import tigase.http.util.AssetsServlet;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@Bean(name = "setup", parent = HttpMessageReceiver.class, active = true)
public class SetupModule extends AbstractBareModule
		implements JaxRsModule<SetupHandler> {

	private static final Logger log = Logger.getLogger(SetupModule.class.getCanonicalName());
	@ConfigField(desc = "Allow access to setup with password", alias = "admin-password")
	private String adminPassword = null;
	@ConfigField(desc = "Allow access to setup for user", alias = "admin-user")
	private String adminUser = null;
	@Inject
	private AuthProvider authProvider;

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
								.isPresent() || authProvider.isAdmin(user);
					}

					@Override
					public boolean checkCredentials(String user, String password)
							throws TigaseStringprepException, TigaseDBException, AuthorizationException {
						if (Objects.equals(user, adminUser) && Objects.equals(password, adminPassword)) {
							return true;
						}
						return authProvider.checkCredentials(user, password);
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
