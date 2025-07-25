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

import java.util.*;
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
	private String globalErrorPage;
	private String name;
	private AuthProvider authProvider;
	private String[] vhosts = null;
	private Map<Class<? extends Throwable>, String> exceptionErrorPages = new HashMap<>();
	private Map<Integer, String> errorCodePages = new HashMap<>();

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

	public AuthProvider getAuthProvider() {
		return authProvider;
	}

	public DeploymentInfo setAuthProvider(AuthProvider authProvider) {
		this.authProvider = authProvider;
		return this;
	}

	public DeploymentInfo addErrorPage(Class<? extends Throwable> exception, String uri) {
		exceptionErrorPages.put(exception, uri);
		return this;
	}

	public DeploymentInfo addErrorPage(int code, String uri) {
		errorCodePages.put(code, uri);
		return this;
	}

	public Map<Class<? extends Throwable>, String> getExceptionErrorPages() {
		return Collections.unmodifiableMap(exceptionErrorPages);
	}

	public Map<Integer, String> getErrorCodePages() {
		return Collections.unmodifiableMap(errorCodePages);
	}

	public String getGlobalErrorPage() {
		return globalErrorPage;
	}

	public DeploymentInfo setGlobalErrorPage(String uri) {
		globalErrorPage = uri;
		return this;
	}

}
