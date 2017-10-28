/*
 * IndexModule.java
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
package tigase.http.modules;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.util.CSSHelper;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by andrzej on 28.05.2016.
 */
@Bean(name = "index", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode, ConfigTypeEnum.ComponentMode})
public class IndexModule extends AbstractModule {

	private static final ConcurrentHashMap<String,IndexModule> modules = new ConcurrentHashMap<>();

	private final String uuid = UUID.randomUUID().toString();

	private DeploymentInfo httpDeployment = null;

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

		super.start();
		modules.put(uuid, this);
		httpDeployment = httpServer.deployment().setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath).setDeploymentName("Index").setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}

		ServletInfo servletInfo = httpServer.servlet("IndexServlet", IndexServlet.class);
		servletInfo.addInitParam("module", uuid);
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
		modules.remove(uuid, this);
		super.stop();
	}

	public static IndexModule getInstance(String uuid) {
		return modules.get(uuid);
	}

	protected List<DeploymentInfo> listDeployments() {
		return httpServer.listDeployed();
	}

	public static class IndexServlet extends HttpServlet {

		private IndexModule module;
		private GStringTemplateEngine templateEngine = new GStringTemplateEngine();
		private Template template = null;

		public IndexServlet() {

		}

		@Override
		public void init() throws ServletException {
			super.init();
			ServletConfig config = super.getServletConfig();
			String uuid = config.getInitParameter("module");
			if (uuid == null)
				throw new ServletException("Missing module parameter!");

			module = IndexModule.getInstance(uuid);
			if (module == null)
				throw new ServletException("Not found module for IndexServlet");

			try {
				loadTemplate();
			} catch (IOException|ClassNotFoundException ex) {
				throw new ServletException("Could not load template", ex);
			}
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			try {
				loadTemplate();
			} catch (IOException|ClassNotFoundException ex) {
				throw new ServletException("Could not load template", ex);
			}

			PrintWriter out = resp.getWriter();
			List<DeploymentInfo> deploymentInfoList = new ArrayList<>(module.listDeployments());
			deploymentInfoList.removeIf(info -> {
				if (info.getVHosts() != null && info.getVHosts().length > 0) {
					List<String> vhosts = Arrays.asList(info.getVHosts());
					return !vhosts.contains(req.getServerName());
				}
				return false;
			});
			deploymentInfoList.sort((o1, o2) -> {
				return o1.getContextPath().compareTo(o2.getContextPath());
			});

			Map model = new HashMap();
			model.put("deployments", deploymentInfoList);
			Map<String, Object> util = new HashMap<>();
			Function<String, String> tmp = (path) -> {
				String content = null;
				try {
					content = CSSHelper.getCssFileContent(path);
				} catch (Exception ex) {}
				if (content == null)
					return "";
				return "<style>" + content + "</style>";
			};
			util.put("inlineCss", tmp);
			model.put("util", util);
			Writable w = template.make(model);
			w.writeTo(out);

//			out.append("Found ").append(""+deploymentInfoList.size()).append(" endpoints");
//			for (DeploymentInfo info : deploymentInfoList) {
//				if (info.getVHosts() != null && info.getVHosts().length > 0) {
//					List<String> vhosts = Arrays.asList(info.getVHosts());
//					if (!vhosts.contains(req.getServerName())) {
//						break;
//					}
//				}
//				out.append("\n").append(info.getDeploymentName()).append(" - ").append(info.getContextPath());
//			}
		}

		private void loadTemplate() throws IOException, ClassNotFoundException {
			String path = "tigase/index/index.html";
			File indexFile = new File(path);
			if (indexFile.exists()) {
				template = templateEngine.createTemplate(indexFile);
			} else {
				InputStream is = getClass().getResourceAsStream("/" + path);
				template = templateEngine.createTemplate(new InputStreamReader(is));
			}
		}
	}
}
