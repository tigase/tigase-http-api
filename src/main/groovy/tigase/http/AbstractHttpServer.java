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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by andrzej on 06.08.2016.
 */
public abstract class AbstractHttpServer implements HttpServerIfc {

	@Inject(bean = "sslContextContainer")
	protected SSLContextContainerIfc sslContextContainer;

	@Inject
	protected PortsConfigBean portsConfigBean;

	protected Kernel kernel;

	protected List<Integer> httpPorts = new CopyOnWriteArrayList<>();
	protected List<Integer> httpsPorts = new CopyOnWriteArrayList<>();


	@Override
	public List<Integer> getHTTPPorts() {
		return Collections.unmodifiableList(httpPorts);
	}

	@Override
	public List<Integer> getHTTPSPorts() {
		return Collections.unmodifiableList(httpsPorts);
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

	public abstract static class PortsConfigBean implements RegistrarBeanWithDefaultBeanClass, Initializable {

		@Inject(nullAllowed = true)
		private PortConfigBean[] portsBeans;

		@ConfigField(desc = "Ports to enable", alias = "ports")
		private HashSet<Integer> ports;

		private Kernel kernel;

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


	public abstract static class PortConfigBean implements ConfigurationChangedAware, UnregisterAware, Initializable {

		@ConfigField(desc = "Port")
		private Integer name;

		@ConfigField(desc = "Socket type")
		private SocketType socket = SocketType.plain;

		@ConfigField(desc = "Certificate for domain is SSL or TLS is enabled")
		private String domain;

		public int getPort() {
			return name;
		}

		public SocketType getSocket() {
			return socket;
		}

		public String getDomain() {
			return domain;
		}
	}

}
