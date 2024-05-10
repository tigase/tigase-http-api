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

import jakarta.ws.rs.FormParam;
import tigase.conf.ConfigBuilder;
import tigase.conf.ConfigWriter;
import tigase.db.util.SchemaLoader;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.server.ConnectionManager;
import tigase.server.xmppsession.SessionManager;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.setup.SetupHelper;
import tigase.util.ui.console.CommandlineParameter;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.jid.BareJID;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static tigase.db.util.DBSchemaLoader.PARAMETERS_ENUM.*;
import static tigase.db.util.DBSchemaLoader.getSupportedTypeForName;
import static tigase.db.util.SchemaLoader.getDefaultSupportedTypeForName;

public class Config {

	private String companyName;
	private ConfigTypeEnum configType = ConfigTypeEnum.DefaultMode;
	private String defaultVirtualDomain = DNSResolverFactory.getInstance().getDefaultHost();
	private Set<BareJID> admins = new HashSet<>();
	private String adminPwd;
	private SchemaLoader.TypeInfo dbType = getSupportedTypeForName(System.getenv(("DB_TYPE"))).orElse(
			getDefaultSupportedTypeForName().orElseThrow());
	private Set<String> connectors = new HashSet<>();
	private Set<String> features = new HashSet<>();
	private boolean clusterMode = false;
	private boolean acs = false;
	private DBConfig dbConfig = DBConfig.getDefaults();
	private SetupHelper.HttpSecurity httpSecurity = new SetupHelper.HttpSecurity();
	private String setupPassword;
	private String setupUser;



	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public ConfigTypeEnum getConfigType() {
		return configType;
	}

	public void setConfigType(ConfigTypeEnum configType) {
		this.configType = configType;
		this.connectors = SetupHelper.getAvailableComponents()
				.stream()
				.filter(def -> !def.isCoreComponent())
				.filter(def -> {
					ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType);
				})
				.filter(def -> ConnectionManager.class.isAssignableFrom(def.getClazz()))
				.filter(def -> def.isActive())
				.map(def -> def.getName())
				.collect(Collectors.toSet());
		Set<String> defComponents = SetupHelper.getAvailableComponents()
				.stream()
				.filter(def -> !def.isCoreComponent())
				.filter(def -> {
					ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType);
				})
				.filter(def -> def.isActive())
				.map(def -> def.getName())
				.collect(Collectors.toSet());
		Set<String> defPlugins = SetupHelper.getAvailableProcessors(SessionManager.class, XMPPImplIfc.class)
				.stream()
				.filter(def -> def.isActive())
				.filter(def -> this.configType == ConfigTypeEnum.DefaultMode ||
						this.configType == ConfigTypeEnum.SessionManagerMode)
				.filter(def -> {
					ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType.name());
				})
				.map(def -> def.getName())
				.collect(Collectors.toSet());

		features.clear();

		if (defComponents.contains("muc")) {
			features.add("muc");
		}
		if (defComponents.contains("pubsub") || defPlugins.stream().filter(it -> it.equals("pep")).findFirst().isPresent()) {
			features.add("pubsub");
		}
		if (defComponents.contains("mix")) {
			features.add("mix");
		}
		if (defComponents.contains("message-archive") || defPlugins.stream().filter(it -> it.startsWith("urn:xmpp:mam:")).findFirst().isPresent()) {
			features.add("mam");
		}
		if (defPlugins.contains("urn:xmpp:push:0")) {
			features.add("push");
		}
		if (defComponents.contains("upload")) {
			features.add("upload");
		}
		if (defPlugins.contains("message-carbons")) {
			features.add("carbons");
		}
		if (defPlugins.contains("urn:xmpp:csi:0")) {
			features.add("csi");
		}
		if (defPlugins.contains("motd")) {
			features.add("motd");
		}
		if (defPlugins.contains("jabber:iq:last-marker")) {
			features.add("lastActivity");
		}
		if (defPlugins.contains("spam-filter")) {
			features.add("spam");
		}
	}

	public String getDefaultVirtualDomain() {
		return defaultVirtualDomain;
	}

	public void setDefaultVirtualDomain(String defaultVirtualDomain) {
		this.defaultVirtualDomain = defaultVirtualDomain;
	}

	public Set<BareJID> getAdmins() {
		if (admins.isEmpty()) {
			// add default admin user
			admins.add(BareJID.bareJIDInstanceNS("admin@" + defaultVirtualDomain));
		}
		return admins;
	}

	public void setAdmins(Set<BareJID> admins) {
		this.admins = admins;
	}

	public String getAdminPwd() {
		return adminPwd;
	}

	public void setAdminPwd(String adminPwd) {
		this.adminPwd = adminPwd;
	}

	public SchemaLoader.TypeInfo getDbType() {
		return dbType;
	}

	public void setDbType(SchemaLoader.TypeInfo dbType) {
		this.dbType = dbType;
	}

	public void setConnectors(Set<String> connectors) {
		this.connectors = connectors;
	}

	public boolean isConnectorEnabled(String name) {
		return this.connectors.contains(name);
	}

	public void setFeatures(Set<String> features) {
		this.features = features;
	}

	public boolean isFeatureEnabled(String name) {
		return this.features.contains(name);
	}

	public boolean isACSEnabled() {
		return isClusterMode() && acs;
	}

	public void setACSEnabled(boolean value) {
		this.acs = value;
	}

	public boolean isClusterMode() {
		return clusterMode;
	}

	public void setClusterMode(boolean clusterMode) {
		this.clusterMode = clusterMode;
	}

	public boolean installationContainsACS() {
		try {
			Class.forName("tigase.licence.LicenceChecker");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public DBConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(DBConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

	public String getSetupUser() {
		return httpSecurity.setupUser;
	}

	public void setSetupUser(String setupUser) {
		this.httpSecurity.setupUser = setupUser;
	}

	public String getSetupPassword() {
		return httpSecurity.setupPassword;
	}

	public void setSetupPassword(String setupPassword) {
		this.httpSecurity.setupPassword = setupPassword;
	}

	public Map<String, Object> getAsMap() {
		// we need to map setup options to actual config file..
		Set<String> components = new HashSet<>(connectors);
		Set<String> plugins = new HashSet<>();

		Set<String> manuallyManagerComponents = Set.of("muc", "pubsub", "mix", "message-archive", "upload");
		Set<String> connectionManagers = SetupHelper.getAvailableComponents()
				.stream()
				.filter(def -> !def.isCoreComponent())
				.filter(def -> {
					ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType);
				})
				.filter(def -> ConnectionManager.class.isAssignableFrom(def.getClazz()))
				.filter(def -> def.isActive())
				.map(def -> def.getName())
				.collect(Collectors.toSet());
		SetupHelper.getAvailableComponents()
				.stream()
				.filter(def -> !def.isCoreComponent())
				.filter(def -> {
					ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType);
				})
				.filter(def -> def.isActive())
				.map(def -> def.getName())
				.filter(name -> !manuallyManagerComponents.contains(name))
				.filter(name -> !connectionManagers.contains(name))
				.forEach(components::add);

		Set<String> manuallyManagedPlugins = Set.of("pep", "urn:xmpp:mam:1", "urn:xmpp:mam:2", "urn:xmpp:push:0",
													"message-carbons", "urn:xmpp:csi:0", "motd",
													"jabber:iq:last-marker", "spam-filter");
		SetupHelper.getAvailableProcessors(SessionManager.class, XMPPImplIfc.class)
				.stream()
				.filter(def -> def.isActive())
				.filter(def -> this.configType == ConfigTypeEnum.DefaultMode ||
						this.configType == ConfigTypeEnum.SessionManagerMode)
				.filter(def -> {
					ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);
					return ct == null || Arrays.asList(ct.value()).contains(this.configType.name());
				})
				.map(def -> def.getName())
				.filter(name -> !manuallyManagedPlugins.contains(name))
				.forEach(plugins::add);

		if (features.contains("muc")) {
			components.add("muc");
		}
		if (features.contains("pubsub")) {
			components.add("pubsub");
			plugins.add("pep");
		}
		if (features.contains("mix")) {
			components.add("mix");
		}
		if (features.contains("mam")) {
			components.add("message-archive");
			plugins.addAll(List.of("urn:xmpp:mam:1", "urn:xmpp:mam:2"));
		}
		if (features.contains("push")) {
			plugins.add("urn:xmpp:push:0");
		}
		if (features.contains("upload")) {
			components.add("upload");
		}
		if (features.contains("carbons")) {
			plugins.add("message-carbons");
		}
		if (features.contains("csi")) {
			plugins.add("urn:xmpp:csi:0");
		}
		if (features.contains("motd")) {
			plugins.add("motd");
		}
		if (features.contains("lastActivity")) {
			plugins.add("jabber:iq:last-marker");
		}
		if (features.contains("spam")) {
			plugins.add("spam-filter");
		}

		ConfigBuilder builder = SetupHelper.generateConfig(configType, dbConfig.getDbUri(dbType), clusterMode, acs,
														   Optional.of(components), Optional.empty(),
														   Optional.of(plugins), defaultVirtualDomain,
														   Optional.ofNullable(admins.stream().toArray(BareJID[]::new)),
														   Optional.ofNullable(httpSecurity));
		return builder.build();
	}

	public String getAsDsl() {
		try {
			Map<String, Object> props = getAsMap();
			StringWriter w = new StringWriter();
			new ConfigWriter().write(w, props);
			return w.toString();
		} catch (IOException ex) {
			return null;
		}
	}

	public static class DBConfig {
		@FormParam("dbName")
		private String dbName;
		@FormParam("dbHost")
		private String dbHost;
		@FormParam("dbUserName")
		private String dbUserName;
		@FormParam("dbUserPassword")
		private String dbUserPassword;
		@FormParam("dbRootName")
		private String dbRootName;
		@FormParam("dbRootPassword")
		private String dbRootPassword;
		@FormParam("dbUseSSL")
		private boolean dbUseSSL = true;
		@FormParam("dbAdditionalOptions")
		private String dbAdditionalOptions;
		@FormParam("useLegacyDatetimeCode")
		private boolean useLegacyDatetimeCode;

		public static DBConfig getDefaults() {
			DBConfig config = new DBConfig();
			config.dbName = DATABASE_NAME.getDefaultValue();
			config.dbHost = System.getenv().getOrDefault("DB_HOST", DATABASE_HOSTNAME.getDefaultValue());
			config.dbUserName = TIGASE_USERNAME.getDefaultValue();
			config.dbUserPassword = TIGASE_PASSWORD.getDefaultValue();
			config.dbRootName = System.getenv().getOrDefault(("DB_ROOT_USER"), ROOT_USERNAME.getDefaultValue());
			config.dbRootPassword = System.getenv().getOrDefault(("DB_ROOT_PASS"), ROOT_PASSWORD.getDefaultValue());
			config.dbUseSSL = Boolean.parseBoolean(USE_SSL.getDefaultValue());
			config.useLegacyDatetimeCode = Boolean.parseBoolean(USE_LEGACY_DATETIME_CODE.getDefaultValue());
			return config;
		}

		public String getDbName() {
			return dbName;
		}

		public void setDbName(String dbName) {
			this.dbName = dbName;
		}

		public String getDbHost() {
			return dbHost;
		}

		public void setDbHost(String dbHost) {
			this.dbHost = dbHost;
		}

		public String getDbUserName() {
			return dbUserName;
		}

		public void setDbUserName(String dbUserName) {
			this.dbUserName = dbUserName;
		}

		public String getDbUserPassword() {
			return dbUserPassword;
		}

		public void setDbUserPassword(String dbUserPassword) {
			this.dbUserPassword = dbUserPassword;
		}

		public String getDbRootName() {
			return dbRootName;
		}

		public void setDbRootName(String dbRootName) {
			this.dbRootName = dbRootName;
		}

		public String getDbRootPassword() {
			return dbRootPassword;
		}

		public void setDbRootPassword(String dbRootPassword) {
			this.dbRootPassword = dbRootPassword;
		}

		public boolean isDbUseSSL() {
			return dbUseSSL;
		}

		public void setDbUseSSL(boolean dbUseSSL) {
			this.dbUseSSL = dbUseSSL;
		}

		public String getDbAdditionalOptions() {
			return dbAdditionalOptions;
		}

		public void setDbAdditionalOptions(String dbAdditionalOptions) {
			this.dbAdditionalOptions = dbAdditionalOptions;
		}

		public boolean hasDbRootCredentials() {
			return (dbRootName != null && !dbRootName.isEmpty()) || (dbRootPassword != null && !dbRootPassword.isEmpty());
		}

		public String getDbUri(SchemaLoader.TypeInfo dbType) {
			if (dbType == null) {
				return null;
			}

			Properties props = new Properties();
			props.put(ROOT_ASK.getName(), ROOT_ASK.getDefaultValue());
			props.put(DATABASE_TYPE.getName(), dbType.getName());
			SchemaLoader loader = SchemaLoader.newInstance(dbType.getName());
			for (CommandlineParameter param : (List<CommandlineParameter>) loader.getSetupOptions()) {
				if (DATABASE_NAME.getName().equals(param.getFullName().get())) {
					props.put(param.getFullName().get(), dbName);
				}
				else if (DATABASE_HOSTNAME.getName().equals(param.getFullName().get())) {
					props.put(param.getFullName().get(), dbHost);
				}
				else if (TIGASE_USERNAME.getName().equals(param.getFullName().get())) {
					props.put(param.getFullName().get(), dbUserName);
				}
				else if (TIGASE_PASSWORD.getName().equals(param.getFullName().get())) {
					props.put(param.getFullName().get(), dbUserPassword);
				}
				else if (DATABASE_OPTIONS.getName().equals(param.getFullName().get())) {
					props.put(param.getFullName().get(), dbAdditionalOptions);
				}
				else if (USE_LEGACY_DATETIME_CODE.getName().equals(param.getFullName().get())) {
					props.put(param.getFullName().get(), useLegacyDatetimeCode);
				}
			}
			SchemaLoader.Parameters parameters = loader.createParameters();
			parameters.setProperties(props);
			parameters.setDbRootAsk(false);
			loader.init(parameters, Optional.empty());
			return loader.getDBUri();
		}
	}
}