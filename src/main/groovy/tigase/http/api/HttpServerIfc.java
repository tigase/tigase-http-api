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
package tigase.http.api;

import tigase.http.DeploymentInfo;
import tigase.http.ServletInfo;
import tigase.kernel.beans.RegistrarBean;

import javax.servlet.http.HttpServlet;
import java.util.List;

/**
 * @author andrzej
 */
public interface HttpServerIfc
		extends RegistrarBean {

	@Deprecated
	public static final String HTTP_PORT_KEY = "port";

	public static final String HTTP_PORTS_KEY = "ports";

	public static final int DEF_HTTP_PORT_VAL = 8080;

	public static final String PORT_SOCKET_KEY = "socket";

	public static final String PORT_DOMAIN_KEY = "domain";
//	public static final String HTTP2_ENABLED_KEY = "http2";

	List<DeploymentInfo> listDeployed();

	void deploy(DeploymentInfo deployment);

	void undeploy(DeploymentInfo deployment);

	default DeploymentInfo deployment() {
		return new DeploymentInfo();
	}

	default ServletInfo servlet(String name, Class<? extends HttpServlet> servletClass) {
		return new ServletInfo(name, servletClass);
	}

	List<Integer> getHTTPPorts();

	List<Integer> getHTTPSPorts();

}
