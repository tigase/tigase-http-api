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
package tigase.http.modules.wellknown;

import tigase.http.DeploymentInfo;
import tigase.http.HttpMessageReceiver;
import tigase.http.ServletInfo;
import tigase.http.modules.AbstractModule;
import tigase.http.modules.admin.Servlet;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.MessageRouter;
import tigase.server.bosh.BoshConnectionManager;
import tigase.server.websocket.WebSocketClientConnectionManager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Bean(name = "well-known", parent = HttpMessageReceiver.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class WellKnownModule extends AbstractModule implements RegistrarBean {

	private DeploymentInfo deployment = null;

	@ConfigField(desc = "Hostname for establishing WS/Bosh connections")
	private String hostname = null;
	private Kernel kernel;

	private List<ServletInfo> wellKnownServlets = List.of(
			new ServletInfo("HostMeta", HostMetaServlet.class).addMapping("/host-meta")
					.addInitParam("format", "xml"),
			new ServletInfo("HostMeta", HostMetaServlet.class).addMapping("/host-meta.json")
					.addInitParam("format", "json"));
	
	@Override
	public String getDescription() {
		return "Support for /.well-known/";
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
		super.register(kernel);
	}

	@Override
	public void unregister(Kernel kernel) {
		super.unregister(kernel);
		this.kernel = null;
	}

	@Override
	public void start() {
		if (deployment != null) {
			stop();
		}

		for (ServletInfo info : wellKnownServlets) {
			info.addInitParam(Servlet.MODULE_ID_KEY, uuid);
		}
		deployment = httpServer.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/.well-known")
				.setDeploymentName("Well-Known")
				.setDeploymentDescription(getDescription())
				.addServlets(wellKnownServlets.toArray(ServletInfo[]::new));
		super.start();
		
		if (vhosts != null) {
			deployment.setVHosts(vhosts);
		}

		httpServer.deploy(deployment);
	}

	@Override
	public void stop() {
		if (deployment != null) {
			httpServer.undeploy(deployment);
			deployment = null;
		}
		super.stop();
	}

	public List<HostMetaServlet.Link> getHostMetaLinks(String domain) {
		String hostname = Optional.ofNullable(this.hostname).orElse(domain);

		Kernel kernel = this.kernel;
		while (kernel.getParent() != null) {
			kernel = kernel.getParent();
		}

		MessageRouter router = kernel.getInstance(MessageRouter.class);
		Stream<HostMetaServlet.Link> wsLinks = router.getComponentsAll()
				.stream()
				.filter(it -> it instanceof WebSocketClientConnectionManager)
				.map(WebSocketClientConnectionManager.class::cast)
				.flatMap(it -> Arrays.stream(it.getPortsConfigBean().getPortsBeans()))
				.map(it -> (it.isSecure() ? "wss" : "ws") + "://" + domain + ":" + it.getPort() + "/")
				.map(HostMetaServlet.WebSocketLink::new);
		Stream<HostMetaServlet.Link> boshLinks = router.getComponentsAll()
				.stream()
				.filter(it -> it instanceof BoshConnectionManager)
				.map(BoshConnectionManager.class::cast)
				.flatMap(it -> Arrays.stream(it.getPortsConfigBean().getPortsBeans()))
				.map(it -> (it.isSecure() ? "https" : "http") + "://" + domain + ":" + it.getPort() + "/")
				.map(HostMetaServlet.BoshLink::new);
		return Stream.concat(wsLinks, boshLinks).collect(Collectors.toList());
	}
}
