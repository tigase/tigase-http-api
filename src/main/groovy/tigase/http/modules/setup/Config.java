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
import tigase.db.util.SchemaLoader;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
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
	private ConfigTypeEnum configType = ConfigTypeEnum.DefaultMode;
	protected String[] virtualDomains = new String[] { tigase.util.DNSResolverFactory.getInstance().getDefaultHost() };
	protected String[] admins = new String[] { "admin@" + tigase.util.DNSResolverFactory.getInstance().getDefaultHost() };
	protected String adminPwd = "tigase";
	protected DbType dbType = null;
	protected boolean advancedConfig = false;
	protected boolean clusterMode = false;

	protected Set<String> optionalComponents = new HashSet<>();
	protected Set<String> plugins = new HashSet<>();

	protected String dbSuperuser = "root";
	protected String dbSuperpass = "";
	protected String dbUser = "tigase";
	protected String dbPass = "tigase12";
	protected String dbName = "tigasedb";
	protected String dbHost = "localhost";
	protected boolean dbUseSSL = false;
	protected String dbParams = "";

	protected RestApiSecurity httpRestApiSecurity = RestApiSecurity.forbidden;
	protected String[] httpRestApiKeys = new String[0];

	protected String setupUser = null;
	protected String setupPassword = null;

	public Config() {
		setConfigType(ConfigTypeEnum.DefaultMode);
	}

	public ConfigTypeEnum getConfigType() {
		return configType;
	}

	public void setConfigType(ConfigTypeEnum configType) {
		if (configType == null) {
			configType = ConfigTypeEnum.DefaultMode;
		}
		this.configType = configType;
		optionalComponents = SetupHelper.getAvailableComponents()
				.stream()
				.filter(def -> !def.isCoreComponent())
				.filter(def -> {
					ConfigType ct = (ConfigType) def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType);
				})
				.filter(def -> def.isActive())
				.map(def -> def.getName())
				.collect(Collectors.toSet());

		plugins = SetupHelper.getAvailableProcessors(SessionManager.class, XMPPImplIfc.class)
				.stream()
				.filter(def -> def.isActive())
				.filter(def -> this.configType == ConfigTypeEnum.DefaultMode || this.configType == ConfigTypeEnum.SessionManagerMode)
				.filter(def -> {
					ConfigType ct = (ConfigType) def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType.name());
				})
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

	public String getAcsName() {
		return acsName;
	}

	public void setAcsName(String acsName) {
		this.acsName = acsName;
	}

	public String getDatabaseUri() {
		if (dbType == null)
			return null;

		Properties props = getSchemaLoaderProperties();
		return SchemaLoader.newInstance(props).getDBUri(props);
//		String uri;
//		switch (dbType) {
//			case Derby:
//				uri =  "jdbc:derby:" + dbName + ";create=true";
//			default:
//				uri = "jdbc:" + dbType.name().toLowerCase() +"://" + dbHost + "/" + dbName + "?user=" + dbUser + "&password=" + dbPass;
//		}
	}

	public Properties getSchemaLoaderProperties() {
		Properties props = new java.util.Properties();
		props.setProperty("schemaVersion", "7-2");

		Config config = this;

		if (config.dbType != null) {
			props.setProperty("dbType", config.dbType.name().toLowerCase());
		}
		if (config.dbUser != null) {
			props.setProperty("dbUser", config.dbUser);
		}
		if (config.dbPass != null) {
			props.setProperty("dbPass", config.dbPass);
		}
		if (config.dbName != null) {
			props.setProperty("dbName", config.dbName);
		}
		if (config.dbSuperuser != null) {
			props.setProperty("rootUser", config.dbSuperuser);
		}
		if (config.dbSuperpass != null) {
			props.setProperty("rootPass", config.dbSuperpass);
		}
		if (config.dbHost != null) {
			props.setProperty("dbHostname", config.dbHost);
		}
		props.setProperty("useSSL", String.valueOf(config.dbUseSSL));
		if (config.admins != null && config.admins.length > 0) {
			props.setProperty("adminJID", Arrays.stream(config.admins).collect(Collectors.joining(",")));
		}
		if (config.adminPwd != null) {
			props.setProperty("adminJIDpass", config.adminPwd);
		}

		return props;
	}

	public Map<String, Object> getConfigurationInMap() throws IOException {
		Map<String, Object> props = new HashMap<>();

		props.put("config-type", configType.id().toLowerCase());
		if (clusterMode) {
			props.put("--cluster-mode", "true");
		}
		props.put("--virt-hosts", Arrays.stream(virtualDomains).collect(Collectors.joining(",")));
		props.put("admin", admins);
		props.put("--debug", "server");

		AbstractBeanConfigurator.BeanDefinition dataSource = createBean("dataSource");
		addBean(dataSource, setBeanProperty(createBean("default"), "uri", getDatabaseUri()));
		addBean(props, dataSource);

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
				.filter(def -> {
					ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);
					if (optionalComponents.contains(def.getName())) {
						return def.isActive() == false || (ct != null && !Arrays.asList(ct.value()).contains(this.configType));
					} else {
						return def.isActive() == true && (ct != null && Arrays.asList(ct.value()).contains(this.configType));
					}
				})
				.forEach(def -> {
					ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);
					addBean(props, createBean(def.getName(), optionalComponents.contains(def.getName()),
											  (ct != null && !Arrays.asList(ct.value()).contains(this.configType)) ? def
													  .getClazz() : null));
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
		return createBean(name, active, null);
	}

	private static AbstractBeanConfigurator.BeanDefinition createBean(String name, boolean active, Class cls) {
		AbstractBeanConfigurator.BeanDefinition def = new AbstractBeanConfigurator.BeanDefinition();
		def.setBeanName(name);
		def.setActive(active);
		if (cls != null) {
			def.setClazzName(cls.getCanonicalName());
		}
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
