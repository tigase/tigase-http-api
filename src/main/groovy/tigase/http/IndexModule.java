/*
 * Tigase HTTP API
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.http;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by andrzej on 28.05.2016.
 */
public class IndexModule extends AbstractModule {

	private static final ConcurrentHashMap<String,IndexModule> modules = new ConcurrentHashMap<>();

	private final String uuid = UUID.randomUUID().toString();
	private String contextPath = null;

	private HttpServer httpServer = null;
	private DeploymentInfo httpDeployment = null;
	private String[] vhosts = null;

	@Override
	public String getName() {
		return "index";
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
		httpDeployment = HttpServer.deployment().setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath).setDeploymentName("Index").setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}

		ServletInfo servletInfo = HttpServer.servlet("IndexServlet", IndexServlet.class);
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

	@Override
	public Map<String, Object> getDefaults() {
		Map<String,Object> props = super.getDefaults();
		props.put(HTTP_CONTEXT_PATH_KEY, "/");
		return props;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		if (props.size() == 1)
			return;
		if (props.containsKey(HTTP_CONTEXT_PATH_KEY)) {
			contextPath = (String) props.get(HTTP_CONTEXT_PATH_KEY);
		}
		if (props.containsKey(HTTP_SERVER_KEY)) {
			httpServer = (HttpServer) props.get(HTTP_SERVER_KEY);
		}
		vhosts = (String[]) props.get(VHOSTS_KEY);
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
