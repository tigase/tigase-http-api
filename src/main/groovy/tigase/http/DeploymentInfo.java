/**
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

import tigase.http.api.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author andrzej
 */
public class DeploymentInfo {

	private static final Logger log = Logger.getLogger(DeploymentInfo.class.getCanonicalName());
	private final Map<String, Object> data = new HashMap<String, Object>();
	private final ArrayList<ServletInfo> servlets = new ArrayList<ServletInfo>();
	private ClassLoader classLoader = null;
	private String contextPath = null;
	private String description = null;
	private String name;
	private Service service;
	private String[] vhosts = null;

	public DeploymentInfo() {
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public DeploymentInfo setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return this;
	}

	public String getContextPath() {
		return contextPath;
	}

	public DeploymentInfo setContextPath(String contextPath) {
		this.contextPath = contextPath;
		return this;
	}

	public String getDeploymentDescription() {
		return description;
	}

	public DeploymentInfo setDeploymentDescription(String description) {
		this.description = description;
		return this;
	}

	public String getDeploymentName() {
		return name;
	}

	public DeploymentInfo setDeploymentName(String name) {
		this.name = name;
		return this;
	}

	public ServletInfo[] getServlets() {
		return servlets.toArray(new ServletInfo[servlets.size()]);
	}

	public DeploymentInfo addServlets(ServletInfo... servlets) {
		this.servlets.addAll(Arrays.asList(servlets));
		return this;
	}

	public String[] getVHosts() {
		return this.vhosts;
	}

	public DeploymentInfo setVHosts(String... vhosts) {
		this.vhosts = vhosts;
		return this;
	}

	public void put(String key, Object value) {
		this.data.put(key, value);
	}

	public <T> T get(String key) {
		return (T) data.get(key);
	}

	public Service getService() {
		return service;
	}

	public DeploymentInfo setService(Service service) {
		this.service = service;
		return this;
	}

}
