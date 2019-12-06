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
import tigase.http.modules.AbstractModule;
import tigase.http.stats.HttpStatsCollector;
import tigase.http.util.StaticFileServlet;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;
import tigase.server.script.CommandIfc;
import tigase.stats.StatisticHolder;
import tigase.stats.StatisticHolderImpl;
import tigase.stats.StatisticsList;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "rest", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class RestModule
		extends AbstractModule {

	private static final Logger log = Logger.getLogger(RestModule.class.getCanonicalName());

	private static final String DEF_SCRIPTS_DIR_VAL = "scripts/rest";

	private static final String SCRIPTS_DIR_KEY = "rest-scripts-dir";
	private static final ConcurrentHashMap<String, StatisticHolder> stats = new ConcurrentHashMap<String, StatisticHolder>();
	private final CommandIfc[] commands = new CommandIfc[] {
			new ReloadHandlersCmd(this), new ApiKeyAddCmd(this),
			new ApiKeyRemoveCmd(this), new ApiKeyUpdateCmd(this)
	};
	private DeploymentInfo httpDeployment = null;
	private List<RestServletIfc> restServlets = new ArrayList<RestServletIfc>();
	@ConfigField(desc = "Scripts directory", alias = SCRIPTS_DIR_KEY)
	private String scriptsDir = DEF_SCRIPTS_DIR_VAL;

	@Inject
	private ApiKeyRepository apiKeyRepository;

	@Inject(nullAllowed = true)
	private List<HttpStatsCollector> statsCollectors = Collections.emptyList();

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

	@Override
	public void everyHour() {
		for (StatisticHolder holder : stats.values()) {
			holder.everyHour();
		}
	}

	@Override
	public void everyMinute() {
		for (StatisticHolder holder : stats.values()) {
			holder.everyMinute();
		}
	}

	@Override
	public void everySecond() {
		for (StatisticHolder holder : stats.values()) {
			holder.everySecond();
		}
	}

	@Override
	public String getDescription() {
		return "REST support - handles HTTP REST access using scripts";
	}

	public void setApiKeyRepository(ApiKeyRepository apiKeyRepository) {
		if (getComponentName() != null) {
			apiKeyRepository.setRepoUser(BareJID.bareJIDInstanceNS(getName(), getComponentName()));
			apiKeyRepository.setRepo(getUserRepository());
		}
		this.apiKeyRepository = apiKeyRepository;
	}
	
	@Override
	public boolean isRequestAllowed(String key, String domain, String path) {
		return apiKeyRepository.isAllowed(key, domain, path);
	}
	
	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}

		super.start();
		httpDeployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath)
				.setService(new tigase.http.ServiceImpl(this))
				.setDeploymentName("REST API")
				.setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}
		File scriptsDirFile = new File(scriptsDir);
		File[] scriptDirFiles = scriptsDirFile.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory() && !"static".equals(file.getName());
			}
		});

		if (scriptDirFiles != null) {
			for (File dirFile : scriptDirFiles) {
				try {
					startRestServletForDirectory(httpDeployment, dirFile);
				} catch (IOException ex) {
					log.log(Level.FINE, "Exception while scanning for scripts to load", ex);
				}
			}
		}

		try {
			ServletInfo servletInfo = httpServer.servlet("RestServlet", RestExtServlet.class);
			servletInfo.addInitParam(RestServlet.REST_MODULE_KEY, uuid)
					.addInitParam(RestServlet.SCRIPTS_DIR_KEY, scriptsDirFile.getCanonicalPath())
					.addMapping("/");
			httpDeployment.addServlets(servletInfo);
		} catch (IOException ex) {
			log.log(Level.FINE, "Exception while scanning for scripts to load", ex);
		}

		ServletInfo servletInfo = httpServer.servlet("StaticServlet", StaticFileServlet.class);
		servletInfo.addInitParam(StaticFileServlet.DIRECTORY_KEY, new File(scriptsDirFile, "static").getAbsolutePath())
				.addMapping("/static/*");
		httpDeployment.addServlets(servletInfo);

		httpServer.deploy(httpDeployment);
	}
	
	@Override
	public void stop() {
		if (httpDeployment != null) {
			httpServer.undeploy(httpDeployment);
			httpDeployment = null;
		}
		Kernel kernel = getKernel(uuid);
		Iterator<BeanConfig> it = kernel.getDependencyManager().getBeanConfigs().iterator();
		while (it.hasNext()) {
			BeanConfig bc = it.next();
			if (Kernel.class.isAssignableFrom(bc.getClazz()) ||
					ApiKeyRepository.class.isAssignableFrom(bc.getClazz()) ||
					RestModule.class.isAssignableFrom(bc.getClazz())) {
				continue;
			}
			if (bc.getState() == BeanConfig.State.initialized) {
				try {
					kernel.unregister(bc.getBeanName());
				} catch (Exception ex) {
					log.log(Level.WARNING, "Could not unregister bean!", ex);
				}
			}
		}
		restServlets = new ArrayList<RestServletIfc>();
		super.stop();
		apiKeyRepository.setAutoloadTimer(0);
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		for (StatisticHolder holder : stats.values()) {
			holder.getStatistics(compName, list);
		}
	}

	public void countRequest(HttpServletRequest request) {
		statsCollectors.forEach(collector -> collector.count(request));
	}

	public void executedIn(String path, long executionTime) {
		StatisticHolder holder = stats.get(path);
		if (holder == null) {
			StatisticHolder tmp = new StatisticHolderImpl();
			tmp.setStatisticsPrefix(getName() + ", path=" + path);
			holder = stats.putIfAbsent(path, tmp);
			if (holder == null) {
				holder = tmp;
			}
		}
		holder.statisticExecutedIn(executionTime);
	}

	public void statisticExecutedIn(long executionTime) {

	}

	public Kernel getKernel() {
		return getKernel(uuid);
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

	protected void registerRestServlet(RestServletIfc servlet) {
		restServlets.add(servlet);
	}

	protected ApiKeyRepository getApiKeyRepository() {
		return apiKeyRepository;
	}

	protected List<? extends RestServletIfc> getRestServlets() {
		return restServlets;
	}

	private void startRestServletForDirectory(DeploymentInfo httpDeployment, File scriptsDirFile) throws IOException {
		File[] scriptFiles = getGroovyFiles(scriptsDirFile);

		if (scriptFiles != null) {
			ServletInfo servletInfo = httpServer.servlet("RestServlet", RestExtServlet.class);
			servletInfo.addInitParam(RestServlet.REST_MODULE_KEY, uuid)
					.addInitParam(RestServlet.SCRIPTS_DIR_KEY, scriptsDirFile.getCanonicalPath())
					.addInitParam("mapping", "/" + scriptsDirFile.getName() + "/*")
					.addMapping("/" + scriptsDirFile.getName() + "/*");
			httpDeployment.addServlets(servletInfo);
		}
	}
}
