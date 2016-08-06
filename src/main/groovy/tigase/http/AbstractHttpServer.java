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
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.net.SocketType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by andrzej on 06.08.2016.
 */
public abstract class AbstractHttpServer implements HttpServerIfc {

	@ConfigField(desc = "Port to bind")
	protected HashSet<Integer> ports = new HashSet<>(Arrays.asList(DEF_HTTP_PORT_VAL));

	@Inject(bean = "sslContextContainer")
	protected SSLContextContainerIfc sslContextContainer;

	protected Kernel kernel;

	protected abstract Class<?> getPortConfigBean();

	public void setPorts(HashSet<Integer> ports) {
		Set<Integer> oldPorts = new HashSet<Integer>(this.ports);
		Set<Integer> newPorts = new HashSet<Integer>(ports);
		newPorts.removeAll(this.ports);
		oldPorts.removeAll(ports);

		this.ports = ports;
		if (kernel == null) {
			return;
		}

		for (int port : oldPorts) {
			unregisterPortConfigBean(port);
		}
		for (int port : newPorts) {
			registerPortConfigBean(port);
		}
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;

		kernel.registerBean(SSLContextContainer.class).exec();

		for (int port : ports) {
			registerPortConfigBean(port);
		}
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	public void registerPortConfigBean(int port) {
		String name = "port/" + port;
		kernel.registerBean(name).asClass(getPortConfigBean()).exec();
		PortConfigBean config = kernel.getInstance(name);
		config.port = port;
		config.beanConfigurationChanged(Collections.singleton("port"));
	}

	public void unregisterPortConfigBean(int port) {
		kernel.unregister("port/" + port);
	}

	public abstract static class PortConfigBean implements ConfigurationChangedAware, UnregisterAware, Initializable {

		private int port;

		@ConfigField(desc = "Socket type")
		private SocketType socket = SocketType.plain;

		@ConfigField(desc = "Certificate for domain is SSL or TLS is enabled")
		private String domain;

		public int getPort() {
			return port;
		}

		public SocketType getSocket() {
			return socket;
		}

		public String getDomain() {
			return domain;
		}
	}

}
