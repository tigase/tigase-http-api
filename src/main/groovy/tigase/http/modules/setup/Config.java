/*
 * Tigase HTTP API
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package tigase.http.modules.setup;

import tigase.conf.ConfigWriter;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.XMPPImplIfc;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 30.03.2017.
 */
public class Config {
	
	private String acsName = "";
	protected boolean acs = true;
	protected ConfigType configType = ConfigType.Default;
	protected String[] virtualDomains = new String[] { tigase.util.DNSResolverFactory.getInstance().getDefaultHost() };
	protected String[] admins = new String[] { "admin@" + tigase.util.DNSResolverFactory.getInstance().getDefaultHost() };
	protected String adminPwd = "tigase";
	protected DbType dbType = null;
	protected boolean advancedConfig = false;
	protected boolean clusterMode = false;

	protected Set<String> optionalComponents;
	protected Set<String> plugins;

	protected String dbSuperuser = "root";
	protected String dbSuperpass = "";
	protected String dbUser = "tigase";
	protected String dbPass = "tigase12";
	protected String dbName = "tigasedb";
	protected String dbHost = "localhost";
	protected String dbParams = "";

	protected RestApiSecurity httpRestApiSecurity = RestApiSecurity.forbidden;
	protected String[] httpRestApiKeys = new String[0];

	protected String setupUser = null;
	protected String setupPassword = null;

	public Config() {
		optionalComponents = SetupHelper.getAvailableComponents()
				.stream()
				.filter(def -> !def.isCoreComponent())
				.filter(def -> def.isActive())
				.map(def -> def.getName())
				.collect(Collectors.toSet());

		plugins = SetupHelper.getAvailableProcessors(SessionManager.class, XMPPImplIfc.class)
				.stream()
				.filter(def -> def.isActive())
				.map(def -> def.getName())
				.collect(Collectors.toSet());
	}

	public List<BeanDefinition> getComponents() {
		return null;
	}

	public void setACS(boolean acs) {
		this.acs = acs;
	}

	public boolean getACS() {
		return acs;
	}

	public static enum ConfigType {
		Default;//,
//		SessionManagerOnly,
//		ConnectionManagersOnly;

		public String getValue() {
			switch (this) {
				case Default:
					return "default";
//				case SessionManagerOnly:
//					return "--gen-config-sm";
//				case ConnectionManagersOnly:
//					return "--gen-config-cs";
			}
			return null;
		}

		public String getLabel() {
			switch (this) {
				case Default:
					return "Default installation";
//				case SessionManagerOnly:
//					return "Session Manager only";
//				case ConnectionManagersOnly:
//					return "Network connectivity only";
			}
			return null;
		}
	}

	public String getAcsName() {
		return acsName;
	}

	public void setAcsName(String acsName) {
		this.acsName = acsName;
	}

	public String getDatabaseUri() {
		if (dbType == null)
			return null;
		switch (dbType) {
			case Derby:
				return "jdbc:derby:" + dbName + ";create=true";
			default:
				return "jdbc:" + dbType.name().toLowerCase() +"://" + dbHost + "/" + dbName + "?user=" + dbUser + "&password=" + dbPass;
		}
	}

	public Map<String, Object> getConfigurationInMap() throws IOException {
		Map<String, Object> props = new HashMap<>();

		props.put("config-type", configType.name().toLowerCase());
		if (clusterMode) {
			props.put("--cluster-mode", "true");
		}
		props.put("--virtual-hosts", Arrays.stream(virtualDomains).collect(Collectors.joining(",")));
		props.put("admin", admins);
		props.put("--debug", "server");

		AbstractBeanConfigurator.BeanDefinition dataSource = createBean("dataSource");
		addBean(props, addBean(dataSource, setBeanProperty(createBean("default"), "uri", getDatabaseUri())));

		AbstractBeanConfigurator.BeanDefinition sessMan = createBean("sess-man");
		addBean(props, sessMan);
		SetupHelper.getAvailableProcessors(SessionManager.class, XMPPImplIfc.class)
				.stream()
				.filter(def -> (def.isActive() && !plugins.contains(def.getName())) ||
						((!def.isActive()) && plugins.contains(def.getName())))
				.forEach(def -> {
					addBean(sessMan, createBean(def.getName(), plugins.contains(def.getName())));
				});

		if (acs) {
			AbstractBeanConfigurator.BeanDefinition strategy = createBean("strategy");
			strategy.setClazzName("tigase.server.cluster.strategy.OnlineUsersCachingStrategy");
			addBean(sessMan, strategy);
		}

		SetupHelper.getAvailableComponents()
				.stream()
				.filter(def -> !def.isCoreComponent())
				.filter(def -> (def.isActive() && !optionalComponents.contains(def.getName())) ||
						((!def.isActive()) && optionalComponents.contains(def.getName())))
				.forEach(def -> {
					addBean(props, createBean(def.getName(), optionalComponents.contains(def.getName())));
				});

		props.compute("http", (name, def) -> {
			if (def == null) {
				def = createBean("http");
			}
			switch (httpRestApiSecurity) {
				case forbidden:
					break;
				case api_keys:
					setBeanProperty((AbstractBeanConfigurator.BeanDefinition) def, "api-keys", httpRestApiKeys);
					break;
				case open_access:
					setBeanProperty((AbstractBeanConfigurator.BeanDefinition) def, "api-keys", Arrays.asList("open_access"));
			}
			if (setupUser != null && !setupUser.isEmpty() && setupPassword != null && !setupPassword.isEmpty()) {
				addBean((AbstractBeanConfigurator.BeanDefinition) def,
						setBeanProperty(setBeanProperty(createBean("setup"), "admin-user", setupUser), "admin-password",
										setupPassword));
			}
			return def;
		});

		return props;
	}

	public String getConfigurationInDSL() throws IOException {
		Map<String, Object> props = getConfigurationInMap();
		StringWriter w = new StringWriter();
		new ConfigWriter().write(w, props);
		return w.toString();
	}

	private static AbstractBeanConfigurator.BeanDefinition addBean(Map<String, Object> props, AbstractBeanConfigurator.BeanDefinition def) {
		props.put(def.getBeanName(), def);
		return def;
	}

	private static AbstractBeanConfigurator.BeanDefinition createBean(String name) {
		return createBean(name, true);
	}

	private static AbstractBeanConfigurator.BeanDefinition createBean(String name, boolean active) {
		AbstractBeanConfigurator.BeanDefinition def = new AbstractBeanConfigurator.BeanDefinition();
		def.setBeanName(name);
		def.setActive(active);
		return def;
	}

	private static AbstractBeanConfigurator.BeanDefinition setBeanProperty(AbstractBeanConfigurator.BeanDefinition def, String key, Object val) {
		def.put(key, val);
		return def;
	}

	public void saveConfig() throws IOException {
		Map<String, Object> props = getConfigurationInMap();
		File f = new File("etc/init.properties");
		if (f.exists()) {
			File bf = new File("etc/init.properties");
			if (!bf.exists()) {
				bf.createNewFile();
			}
			try (FileOutputStream fos = new FileOutputStream(bf, false); FileInputStream fis = new FileInputStream(f)) {
				byte[] tmp = new byte[1024];
				int read = 0;
				while ((read = fis.read(tmp)) > -1) {
					fos.write(tmp, 0, read);
				}
			}
		} else {
			f.createNewFile();
		}
		new ConfigWriter().write(f, props);
	}

	public static enum DbType {
		Derby,
		MySQL,
		PostgreSQL,
		SQLServer,
		Other
	}

	public static enum RestApiSecurity {
		forbidden,
		api_keys,
		open_access
	}

}
