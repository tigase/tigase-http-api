/*
 * Tigase HTTP API
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
 */
package tigase.http.api;

import java.util.Map;
import tigase.http.DeploymentInfo;

/**
 *
 * @author andrzej
 */
public interface HttpServerIfc {

	@Deprecated
	public static final String HTTP_PORT_KEY = "port";
	public static final String HTTP_PORTS_KEY = "ports";
	public static final int DEF_HTTP_PORT_VAL = 8080;
	public static final String PORT_SOCKET_KEY = "socket";
	public static final String PORT_DOMAIN_KEY = "domain";
	
	void start();
	void stop();
	void deploy(DeploymentInfo deployment);
	void undeploy(DeploymentInfo deployment);
	void setProperties(Map<String,Object> props);
	
}
