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
package tigase.http;

import tigase.http.api.HttpServerIfc;
import tigase.io.SSLContextContainer;
import tigase.io.SSLContextContainerIfc;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBeanWithDefaultBeanClass;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.net.SocketType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by andrzej on 06.08.2016.
 */
public abstract class AbstractHttpServer
		implements HttpServerIfc {

	private static final ConcurrentHashMap<String, AbstractHttpServer> SERVERS = new ConcurrentHashMap<>();

	public static Optional<PortConfigBean> getPortConfig(String name, int port) {
		return Optional.ofNullable(SERVERS.get(name))
				.flatMap(server -> Arrays.stream(server.portsConfigBean.portsBeans)
						.filter(config -> port == config.name)
						.findFirst());
	}
	
	protected List<Integer> httpPorts = new CopyOnWriteArrayList<>();
	protected List<Integer> httpsPorts = new CopyOnWriteArrayList<>();
	protected Kernel kernel;
	@ConfigField(desc = "Name of the bean")
	private String name;
	@Inject
	protected PortsConfigBean portsConfigBean;
	@Inject(bean = "sslContextContainer")
	protected SSLContextContainerIfc sslContextContainer;

	@Override
	public List<Integer> getHTTPPorts() {
		return Collections.unmodifiableList(httpPorts);
	}

	@Override
	public List<Integer> getHTTPSPorts() {
		return Collections.unmodifiableList(httpsPorts);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;

		kernel.registerBean(SSLContextContainer.class).exec();

	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	@Override
	public void initialize() {
		SERVERS.put(name, this);
	}

	@Override
	public void beforeUnregister() {
		SERVERS.remove(name);
	}

	public abstract static class PortConfigBean
			implements ConfigurationChangedAware, UnregisterAware, Initializable {

		@ConfigField(desc = "Certificate for domain is SSL or TLS is enabled")
		private String domain;
		@ConfigField(desc = "Port")
		private Integer name;
		@ConfigField(desc = "Socket type")
		private SocketType socket = SocketType.plain;
		@ConfigField(desc = "Redirect URI")
		private String redirectUri;
		@ConfigField(desc = "Redirection condition")
		private RedirectionCondition redirectCondition = RedirectionCondition.never;

		public int getPort() {
			return name;
		}

		public SocketType getSocket() {
			return socket;
		}

		public String getDomain() {
			return domain;
		}

		public String getRedirectUri() {
			return redirectUri;
		}

		public RedirectionCondition getRedirectCondition() {
			return redirectCondition;
		}

		public enum RedirectionCondition {
			never,
			http,
			https,
			always
		}
	}

	public abstract static class PortsConfigBean
			implements RegistrarBeanWithDefaultBeanClass, Initializable {

		private Kernel kernel;
		@ConfigField(desc = "Ports to enable", alias = "ports")
		private HashSet<Integer> ports;
		@Inject(nullAllowed = true)
		private PortConfigBean[] portsBeans;

		public PortsConfigBean() {

		}

		@Override
		public void register(Kernel kernel) {
			this.kernel = kernel;
			String connManagerBean = kernel.getParent().getName();
			this.kernel.getParent().ln("service", kernel, connManagerBean);
		}

		@Override
		public void unregister(Kernel kernel) {
			this.kernel = null;
		}

		@Override
		public void initialize() {
			if (ports == null) {
				ports = new HashSet(Arrays.asList(8080));
			}

			for (Integer port : ports) {
				String name = String.valueOf(port);
				if (kernel.getDependencyManager().getBeanConfig(name) == null) {
					kernel.registerBean(name).asClass(getDefaultBeanClass()).exec();
				}
			}

			register(kernel);
		}
	}

}
