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
package tigase.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.http.api.Service;

/**
 *
 * @author andrzej
 */
public class DeploymentInfo {

	private static final Logger log = Logger.getLogger(DeploymentInfo.class.getCanonicalName());
	
	private ClassLoader classLoader = null;
	private String contextPath = null;
	private String name;
	private String[] vhosts = null;
	private final ArrayList<ServletInfo> servlets = new ArrayList<ServletInfo>();
	
	private final Map<String,Object> data = new HashMap<String,Object>();
	private Service service;
	
	public DeploymentInfo() {}
		
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
	
	public DeploymentInfo setService(Service service) {
		this.service = service;
		return this;
	}
	
	public Service getService() {
		return service;
	}
	
}
