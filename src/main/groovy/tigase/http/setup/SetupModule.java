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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.http.setup;

import tigase.http.AbstractModule;
import tigase.http.DeploymentInfo;
import tigase.http.HttpServer;
import tigase.http.ServletInfo;
import tigase.http.api.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
public class SetupModule extends AbstractModule {

	private static final Logger log = Logger.getLogger(SetupModule.class.getCanonicalName());
	
	private static final String NAME = "setup";
	
	private String contextPath = null;
	
	private HttpServer httpServer = null;
	private DeploymentInfo httpDeployment = null;

	private String[] vhosts = null;
	private Service service = null;

	private final String uuid = UUID.randomUUID().toString();
	private static final ConcurrentHashMap<String,AbstractModule> modules = new ConcurrentHashMap<String,AbstractModule>();
	
	public static AbstractModule getModuleByUUID(String uuid) {
		return modules.get(uuid);
	}	
		
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Setup - handles basic configuration of Tigase XMPP Server";
	}
	
	@Override
	public void start() {
		if (httpDeployment != null) {
			stop();
		}
	
		super.start();
		service = new tigase.http.ServiceImpl(this);
		modules.put(uuid, this);
		httpDeployment = HttpServer.deployment().setClassLoader(this.getClass().getClassLoader())
				.setContextPath(contextPath).setService(service).setDeploymentName("Setup").setDeploymentDescription(getDescription());
		if (vhosts != null) {
			httpDeployment.setVHosts(vhosts);
		}
		ServletInfo servletInfo = HttpServer.servlet("SetupServlet", SetupServlet.class).addInitParam("module", uuid);
		servletInfo.addMapping("/*");
		httpDeployment.addServlets(servletInfo);
		httpServer.deploy(httpDeployment);
	}
	
	@Override
	public void stop() {
		if (httpDeployment != null) { 
			httpServer.undeploy(httpDeployment);
			modules.remove(uuid, this);
			httpDeployment = null;
		}
		super.stop();
	}	

	@Override
	public Map<String, Object> getDefaults() {
		Map<String,Object> props = super.getDefaults();
		props.put(HTTP_CONTEXT_PATH_KEY, "/" + getName());
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
}
