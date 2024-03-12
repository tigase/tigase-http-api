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
package tigase.http.modules.setup;

import tigase.conf.ConfigBuilder;
import tigase.conf.ConfigHolder;
import tigase.conf.ConfigWriter;
import tigase.db.util.DBSchemaLoader;
import tigase.db.util.SchemaLoader;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.server.xmppsession.SessionManager;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.setup.SetupHelper;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.jid.BareJID;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 30.03.2017.
 */
public class Config {

	protected boolean acs = true;
	public String adminPwd = "tigase";
	public BareJID[] admins = new BareJID[]{
			BareJID.bareJIDInstanceNS("admin@" + DNSResolverFactory.getInstance().getDefaultHost())};
	public boolean advancedConfig = false;
	private boolean clusterMode = false;
	public Properties dbProperties = new Properties();
	public SetupHelper.HttpSecurity httpSecurity = new SetupHelper.HttpSecurity();
	public Set<String> optionalComponents = new HashSet<>();
	public Set<String> plugins = new HashSet<>();
	public String defaultVirtualDomain = DNSResolverFactory.getInstance().getDefaultHost();
	private String acsName = "";
	private ConfigTypeEnum configType = ConfigTypeEnum.DefaultMode;
	private String dbType = null;

	private static AbstractBeanConfigurator.BeanDefinition addBean(Map<String, Object> props,
																   AbstractBeanConfigurator.BeanDefinition def) {
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

	private static AbstractBeanConfigurator.BeanDefinition setBeanProperty(AbstractBeanConfigurator.BeanDefinition def,
																		   String key, Object val) {
		def.put(key, val);
		return def;
	}

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
				.filter(def -> this.configType == ConfigTypeEnum.DefaultMode ||
						this.configType == ConfigTypeEnum.SessionManagerMode)
				.filter(def -> {
					ConfigType ct = (ConfigType) def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType.name());
				})
				.map(def -> def.getName())
				.collect(Collectors.toSet());
	}

	public String getDbType() {
		return dbType;
	}

	public void setDbType(String type) {
		this.dbType = type;
		this.dbProperties.setProperty("dbType", type);
	}

	public boolean getACS() {
		return acs;
	}

	public void setACS(boolean acs) {
		this.acs = acs;
	}

	public String getAcsName() {
		return acsName;
	}

	public void setAcsName(String acsName) {
		this.acsName = acsName;
	}

	public boolean getClusterMode() {
		return clusterMode;
	}

	public void setClusterMode(boolean val) {
		this.clusterMode = val;
	}
	
	public String getDatabaseUri() {
		if (dbType == null) {
			return null;
		}

		Properties props = getSchemaLoaderProperties();

		SchemaLoader loader = SchemaLoader.newInstance(dbType);
		SchemaLoader.Parameters parameters = loader.createParameters();
		parameters.setProperties(props);
		loader.init(parameters, Optional.empty());
		return loader.getDBUri();
	}

	public Properties getSchemaLoaderProperties() {
		dbProperties.setProperty(DBSchemaLoader.PARAMETERS_ENUM.ROOT_ASK.getName(), "false");
		return dbProperties;
	}

	public Map<String, Object> getConfigurationInMap() throws IOException {
		ConfigBuilder builder = SetupHelper.generateConfig(configType, getDatabaseUri(), clusterMode, acs,
														   Optional.of(optionalComponents), Optional.empty(),
														   Optional.of(plugins), defaultVirtualDomain,
														   Optional.ofNullable(admins),
														   Optional.ofNullable(httpSecurity));
		return builder.build();
	}

	public String getConfigurationInDSL() throws IOException {
		Map<String, Object> props = getConfigurationInMap();
		StringWriter w = new StringWriter();
		new ConfigWriter().write(w, props);
		return w.toString();
	}

	public void saveConfig() throws IOException {
		Map<String, Object> props = getConfigurationInMap();
		File f = new File(ConfigHolder.TDSL_CONFIG_FILE_DEF);
		if (f.exists()) {
			File bf = new File(ConfigHolder.TDSL_CONFIG_FILE_DEF);
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

}
