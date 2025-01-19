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

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.WriterOutput;
import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.util.AssetsServlet;
import tigase.http.util.TemplateUtils;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by andrzej on 28.05.2016.
 */
@Bean(name = "index", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class IndexModule
		extends AbstractModule {

	private static final ConcurrentHashMap<String, IndexModule> modules = new ConcurrentHashMap<>();

	private final String uuid = UUID.randomUUID().toString();

	private DeploymentInfo httpDeployment = null;

	public static IndexModule getInstance(String uuid) {
		return modules.get(uuid);
	}

	private TemplateEngine templateEngine;

	public IndexModule() {
		contextPath = "/";
	}

	public void setName(String name) {
		this.name = name;
		contextPath = "/";
	}

	@Override
	public String getDescription() {
		return "Index of all available HTTP endpoints";
	}

	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}

		templateEngine =  TemplateUtils.create(null, "tigase.index", ContentType.Html);
		super.start();
		modules.put(uuid, this);
		httpDeployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath)
				.setDeploymentName("Index")
				.setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}

		ServletInfo servletInfo = httpServer.servlet("AssetsServlet", AssetsServlet.class);
		servletInfo.addMapping("/assets/*");
		httpDeployment.addServlets(servletInfo);

		servletInfo = httpServer.servlet("IndexServlet", IndexServlet.class);
		servletInfo.addInitParam("module", uuid);
		servletInfo.addMapping("/");
		httpDeployment.addServlets(servletInfo);

		httpServer.deploy(httpDeployment);
	}

	@Override
	public void stop() {
		templateEngine = null;
		if (httpDeployment != null) {
			httpServer.undeploy(httpDeployment);
			httpDeployment = null;
		}
		modules.remove(uuid, this);
		super.stop();
	}

	protected List<DeploymentInfo> listDeployments() {
		return httpServer.listDeployed();
	}

	public static class IndexServlet
			extends HttpServlet {

		private IndexModule module;
		private TemplateEngine engine = null;

		public IndexServlet() {

		}

		@Override
		public void init() throws ServletException {
			super.init();
			ServletConfig config = super.getServletConfig();
			String uuid = config.getInitParameter("module");
			if (uuid == null) {
				throw new ServletException("Missing module parameter!");
			}

			module = IndexModule.getInstance(uuid);
			if (module == null) {
				throw new ServletException("Not found module for IndexServlet");
			}

			engine = module.templateEngine;
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			PrintWriter out = resp.getWriter();
			List<DeploymentInfo> deploymentInfoList = new ArrayList<>(module.listDeployments());
			deploymentInfoList.removeIf(info -> {
				if (info.getVHosts() != null && info.getVHosts().length > 0) {
					List<String> vhosts = Arrays.asList(info.getVHosts());
					return !vhosts.contains(req.getServerName());
				}
				return false;
			});
			deploymentInfoList.sort(Comparator.comparing(DeploymentInfo::getContextPath));

			Map model = new HashMap();
			model.put("deployments", deploymentInfoList);

			engine.render("index.jte", model, new WriterOutput(out));
		}
		
	}
}
