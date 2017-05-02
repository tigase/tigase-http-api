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

import tigase.db.util.SchemaLoader;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.server.BasicComponent;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.XMPPImplIfc;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andrzej on 30.03.2017.
 */
public class Setup {

	private final Config config = new Config();
	private final List<Page> pages = new ArrayList<>();

	public Setup() {
		pages.add(new Page("About software"));
		pages.add(new Page("Advanced Clustering Strategy information",
						   new SingleAnswerQuestion("acsName", config::getAcsName, config::setAcsName)));

		pages.add(new Page("Basic Tigase server configuration",
						   new SingleAnswerQuestion("configType", () -> config.getConfigType().id(), type -> config.setConfigType(ConfigTypeEnum
								   .valueForId(type))),
						   new VirtualDomainsQuestion("virtualDomains", config),
						   new AdminsQuestion("admins", config), new SingleAnswerQuestion("adminPwd", ()-> config.adminPwd, pwd -> config.adminPwd = pwd),
						   new SingleAnswerQuestion("dbType", ()-> config.dbType != null ? config.dbType.name() : null, type -> config.dbType = (type == null ? null : Config.DbType.valueOf(type))),
						   new SingleAnswerQuestion("advancedConfig", () -> String.valueOf(config.advancedConfig), val -> config.advancedConfig = val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false)
						   ));
		pages.add(new AdvConfigPage("Advanced configuration options", config, Stream.of(
				new SingleAnswerQuestion("clusterMode", () -> String.valueOf(config.clusterMode), val -> config.clusterMode = val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false),
				new SingleAnswerQuestion("acsComponent", () -> String.valueOf(config.acs), val -> config.acs = val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false)
		)));

		pages.add(new PluginsConfigPage("Plugins selection", config));

		pages.add(new Page("Database configuration",
						   new SingleAnswerQuestion("dbSuperuser", ()-> config.dbSuperuser, user -> config.dbSuperuser = user),
						   new SingleAnswerQuestion("dbSuperpass", ()-> config.dbSuperpass, pass -> config.dbSuperpass = pass),
						   new SingleAnswerQuestion("dbUser", ()-> config.dbUser, user -> config.dbUser = user),
						   new SingleAnswerQuestion("dbPass", ()-> config.dbPass, pass -> config.dbPass = pass),
						   new SingleAnswerQuestion("dbName", ()-> config.dbName, name -> config.dbName = name),
						   new SingleAnswerQuestion("dbHost", ()-> config.dbHost, host -> config.dbHost = host),
						   new SingleAnswerQuestion("dbUseSSL", ()-> String.valueOf(config.dbUseSSL), val -> config.dbUseSSL = val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false),
						   new SingleAnswerQuestion("dbParams", ()-> config.dbParams, params -> config.dbParams = params)
		));

		pages.add(new DBCheckPage("Database connectivity check"));

		pages.add(new Page("HTTP API - REST security configuration",
				  new SingleAnswerQuestion("httpRestApiSecurity", ()-> config.httpRestApiSecurity.name(),
										   val -> config.httpRestApiSecurity = Config.RestApiSecurity.valueOf(val)),
				  new SingleAnswerQuestion("httpRestApiKeys", ()-> Arrays.stream(config.httpRestApiKeys).collect(Collectors.joining(",")),
										   (val)-> config.httpRestApiKeys = val == null ?  new String[0] : val.split(","))));

		pages.add(new Page("Setup security",
						   new SingleAnswerQuestion("setupUser", ()-> config.setupUser, user -> config.setupUser = user),
						   new SingleAnswerQuestion("setupPassword", ()-> config.setupPassword, pass -> config.setupPassword = pass)));

		pages.add(new Page("Saving configuration", new SingleAnswerQuestion("saveConfig", ()-> "true", val -> {
			if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
				try {
					config.saveConfig();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		})));
		pages.add(new Page("Finished"));
	}
	
	public Page getPage(int page) {
		return pages.get(page - 1);
	}

	public class Page {

		private final String title;
		private final Map<String, Question> questions = new ConcurrentHashMap<>();

		public Page(String title, Stream<Question> questions) {
			this.title = title;
			questions.forEach(this::addQuestion);
		}

		public Page(String title, Question... questions) {
//			this.title = title;
//			this.questions.putAll(
//					Arrays.stream(questions).collect(Collectors.toMap(Question::getId, Function.identity())));
//			this.questions.values().forEach(Question::setPage);
//			Arrays.stream(questions).forEach(this::addQuestion);
			this(title, Arrays.stream(questions));
		}

		protected void addQuestion(Question question) {
			question.setPage(this);
			this.questions.put(question.getId(), question);
		}

		public String getId() {
			return "" + Setup.this.pages.indexOf(this);
		}

		public String getTitle() {
			return title;
		}

		public Question getQuestion(String id) {
			return questions.get(id);
		}

		public Integer nextPage() {
			int i = Setup.this.pages.indexOf(this);
			return i+1+1;
		}

		protected void setValues(Map<String,String[]> params) {
			questions.values().forEach(question -> {
				String[] value = params.get(question.getName());
				question.setValues(value);
			});
		}
	}

	public abstract static class Question {

		private final String id;
		private Page page;

		public Question(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return page.getId() + "_" + getId();
		}

		protected void setPage(Page page) {
			this.page = page;
		}

		protected abstract void setValues(String[] values);

	}

	public static class SingleAnswerQuestion extends Question {

		private final Supplier<String> getter;
		private final Consumer<String> setter;

		public SingleAnswerQuestion(String id, Supplier<String> getter, Consumer<String> setter) {
			super(id);
			this.getter = getter;
			this.setter = setter;
		}

		public String getValue() {
			return getter.get();
		}

		public void setValue(String value) {
			setter.accept(value);
		}

		@Override
		protected void setValues(String[] values) {
			setValue((values == null || values.length == 0) ? null : values[0]);
		}

		public boolean isSelected(String value) {
			return value != null && value.equals(getValue());
		}
	}

	public static class MultiAnswerQuestion extends Question {

		private final Supplier<String[]> getter;
		private final Consumer<String[]> setter;

		public MultiAnswerQuestion(String id, Supplier<String[]> getter, Consumer<String[]> setter) {
			super(id);
			this.getter = getter;
			this.setter = setter;
		}

		public String[] getValues() {
			return getter.get();
		}

		public void setValues(String[] value) {
			setter.accept(value);
		}

	}

	private class AdminsQuestion extends SingleAnswerQuestion {
		AdminsQuestion(String id, Config config) {
			super(id, ()-> Arrays.stream(config.admins).collect(
					Collectors.joining(",")), admins -> {
				if (admins != null) {
					config.admins = admins.split(",");
				} else {
					config.admins = new String[0];
				}
			});
		}
	}

	private class VirtualDomainsQuestion extends SingleAnswerQuestion {
		VirtualDomainsQuestion(String id, Config config) {
			super(id, ()-> Arrays.stream(config.virtualDomains).collect(
					Collectors.joining(",")), vhosts -> {
				if (vhosts != null) {
					config.virtualDomains = vhosts.split(",");
				} else {
					config.virtualDomains = new String[0];
				}
			});
		}
	}

	private class AdvConfigPage extends Page {

		private List<ComponentQuestion> optionalComponents = new ArrayList<>();

		public AdvConfigPage(String title, Config config, Stream<Question> questions) {
			super(title, questions);

			optionalComponents = SetupHelper.getAvailableComponents()
					.stream()
					.filter(def -> !def.isCoreComponent())
					.map(def -> new ComponentQuestion(def, config))
					.sorted(ComponentQuestion.byBeanName)
					.collect(Collectors.toList());
			optionalComponents.forEach(this::addQuestion);
		}

		public List<Question> getOptionalComponents() {
			return Collections.unmodifiableList(optionalComponents);
		}
	}

	private static class ComponentQuestion extends SingleAnswerQuestion {

		private static final Comparator<ComponentQuestion> byBeanName = (e1,e2) -> e1.getBeanName().compareTo(e2.getBeanName());

		private final BeanDefinition def;

		ComponentQuestion(BeanDefinition def, Config config) {
			super(def.getName(), ()-> config.optionalComponents.contains(def.getName()) ? def.getName() : null, (val) -> {
				if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
					config.optionalComponents.add(def.getName());
				} else {
					config.optionalComponents.remove(def.getName());
				}
			});
			this.def = def;
		}

		public String getLabel() {
			String desc = def.getName();
			try {
				desc = ((BasicComponent) def.getClazz().newInstance()).getDiscoDescription();
			} catch (Exception ex) {
			}
			return desc;
		}

		public String getBeanName() {
			return def.getName();
		}

	}

	private class PluginsConfigPage extends Page {

		private List<PluginQuestion> plugins;

		PluginsConfigPage(String title, Config config) {
			super(title, Stream.empty());

			plugins = SetupHelper.getAvailableProcessors(SessionManager.class, XMPPImplIfc.class)
					.stream()
					.map(def -> new PluginQuestion(def, config))
					.sorted(PluginQuestion.byBeanName)
					.collect(Collectors.toList());
			plugins.forEach(this::addQuestion);
		}

		public List<Question> getPlugins() {
			return Collections.unmodifiableList(plugins);
		}
	}

	private static class PluginQuestion extends SingleAnswerQuestion {

		private static final Comparator<PluginQuestion> byBeanName = (e1, e2) -> e1.getBeanName().compareTo(e2.getBeanName());

		private final BeanDefinition def;
		private final Config config;

		PluginQuestion(BeanDefinition def, Config config) {
			super(def.getName(), ()-> config.plugins.contains(def.getName()) ? def.getName() : null, (val) -> {
				if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
					config.plugins.add(def.getName());
				} else {
					config.plugins.remove(def.getName());
				}
			});
			this.def = def;
			this.config = config;
		}

		public String getLabel() {
			return null;
		}

		public String getBeanName() {
			return def.getName();
		}

		public boolean mayBeChanged() {
			if (!mayBeEnabled()) {
				return false;
			}
			if (def.getClazz().getCanonicalName().equals("tigase.archive.unified.processors.UnifiedArchivePlugin") &&
					config.optionalComponents.contains("unified-archive")) {
				return false;
			}
			return true;
		}

		public boolean mayBeEnabled() {
			if (def.getClazz().getCanonicalName().startsWith("tigase.archive.unified")) {
				return config.optionalComponents.contains("unified-archive");
			}
			if (def.getClazz().getCanonicalName().startsWith("tigase.archive.")) {
				return config.optionalComponents.contains("message-archive") ||
						config.optionalComponents.contains("unified-archive");
			}
			if (def.getClazz().getCanonicalName().startsWith("tigase.pubsub.")) {
				return config.optionalComponents.contains("pubsub");
			}
			return true;
		}

		public String getCause() {
			if (def.getClazz().getCanonicalName().startsWith("tigase.archive.unified")) {
				return "Processor requires Tigase Unified Archive Component";
			}
			if (def.getClazz().getCanonicalName().startsWith("tigase.archive.")) {
				return "Processor requires Tigase Message Archiving Component or Tigase Unified Component";
			}
			if (def.getClazz().getCanonicalName().startsWith("tigase.pubsub.")) {
				return "Processor requires Tigase PubSub Component";
			}
			return null;
		}

		@Override
		public boolean isSelected(String value) {
			if (def.getClazz().getCanonicalName().equals("tigase.archive.unified.processors.UnifiedArchivePlugin")) {
				return config.optionalComponents.contains("unified-archive");
			}
			return super.isSelected(value);
		}
	}

	private class DBCheckPage extends Page {

		public DBCheckPage(String title, Stream<Question> questions) {
			super(title, questions);
		}

		public DBCheckPage(String title, Question... questions) {
			super(title, questions);
		}

		public synchronized List<Entry> loadSchema() {
			Properties props = config.getSchemaLoaderProperties();
			SchemaLoader loader = SchemaLoader.newInstance(props);
			Logger logger = java.util.logging.Logger.getLogger(loader.getClass().getCanonicalName());
			SetupLogHandler handler = Arrays.stream(logger.getHandlers())
					.filter(h -> h instanceof SetupLogHandler)
					.map(h -> (SetupLogHandler) h)
					.findAny()
					.orElseGet(() -> {
						SetupLogHandler handler1 = new SetupLogHandler();
						logger.addHandler(handler1);
						return handler1;
					});
			handler.setLevel(java.util.logging.Level.FINEST);
			logger.setLevel(java.util.logging.Level.FINEST);

			List<Function<SchemaLoader,Entry>> operations = getOperations(props, handler);
			List<Entry> results = new ArrayList<>();

			for (Function<SchemaLoader,Entry> operation : operations) {
				Entry entry = operation.apply(loader);
				results.add(entry);
//				if (entry.result == SchemaLoader.Result.error) {
//					break;
//				}
			}
			loader.shutdown(props);

			return results;
		}

		private List<Function<SchemaLoader,Entry>> getOperations(Properties props, SetupLogHandler logHandler) {
			List<Function<SchemaLoader,Entry>> operations = new ArrayList<>();

			operations.add(loader -> new Entry("Checking connection to database", loader.validateDBConnection(props),
											   logHandler));
			operations.add(loader -> new Entry("Checking if database exists", loader.validateDBExists(props),
											   logHandler));
			operations.add(loader -> new Entry("Checking database schema", loader.validateDBSchema(props),
											   logHandler));
			operations.add(loader -> new Entry("Adding XMPP admin accounts", loader.addXmppAdminAccount(props),
											   logHandler));

			if (config.optionalComponents.contains("muc")) {
				String version = getComponentSchemaVersion("muc");
				operations.add(loader -> {
					props.setProperty("file", "database/" + props.get("dbType").toString()+"-muc-schema-" + version + ".sql");
					return new Entry("Loading MUC component schema", loader.loadSchemaFile(props), logHandler);
				});
			}
			if (config.optionalComponents.contains("pubsub")) {
				String version = getComponentSchemaVersion("pubsub");
				operations.add(loader -> {
					props.setProperty("file", "database/" + props.get("dbType").toString()+"-pubsub-schema-" + version + ".sql");
					return new Entry("Loading PubSub component schema", loader.loadSchemaFile(props), logHandler);
				});
			}
			if (config.optionalComponents.contains("message-archive")) {
				String version = getComponentSchemaVersion("message-archive");
				operations.add(loader -> {
					props.setProperty("file", "database/" + props.get("dbType").toString()+"-message-archiving-schema-" + version + ".sql");
					return new Entry("Loading Message Archiving component schema", loader.loadSchemaFile(props), logHandler);
				});
			}
			if (config.optionalComponents.contains("unified-archive")) {
				String version = getComponentSchemaVersion("unified-archive");
				operations.add(loader -> {
					props.setProperty("file", "database/" + props.get("dbType").toString()+"-unified-archive-schema-" + version + ".sql");
					return new Entry("Loading Unified Archive component schema", loader.loadSchemaFile(props), logHandler);
				});
			}
			if (config.optionalComponents.contains("http") || config.optionalComponents.contains("upload")) {
				String version = getComponentSchemaVersion("http");
				operations.add(loader -> {
					props.setProperty("file", "database/" + props.get("dbType").toString()+"-http-api-schema-" + version + ".sql");
					return new Entry("Loading HTTP API and Upload components schema", loader.loadSchemaFile(props), logHandler);
				});
			}
			if (config.optionalComponents.contains("socks5")) {
				operations.add(loader -> {
					props.setProperty("file", "database/" + props.get("dbType").toString()+"-socks5-schema.sql");
					return new Entry("Loading Socks5 component schema", loader.loadSchemaFile(props), logHandler);
				});
			}

			operations.add(loader -> new Entry("Post installation action", loader.postInstallation(props), logHandler));

			return operations;
		}

		private String getComponentSchemaVersion(String compName) {
			String version = SetupHelper.getAvailableComponents()
					.stream()
					.filter(def -> def.getName().equals(compName))
//					.filter(def -> !ClusteredComponentIfc.class.isAssignableFrom(def.getClazz()))
					.map(def -> def.getClazz())
					.map(cls -> cls.getPackage().getImplementationVersion())
					.filter(ver -> ver != null && !ver.isEmpty())
					.map(ver -> ver.split("-")[0])
					.findAny()
					.get();
			return version;
		}
		
		private class Entry {

			public final String name;
			public final SchemaLoader.Result result;
			public final String message;

			private Entry(String name, SchemaLoader.Result result, SetupLogHandler logHandler) {
				this.name = name;
				this.result = result;
				LogRecord rec;
				StringBuilder sb = null;
				while ((rec = logHandler.poll()) != null) {
					if (rec.getLevel().intValue() <= Level.FINE.intValue()) {
						continue;
					}
					if (rec.getMessage() == null) {
						continue;
					}
					if (sb == null) {
						sb = new StringBuilder();
					} else {
						sb.append("\n");
					}
					sb.append(String.format(rec.getMessage(), rec.getParameters()));
				}
				this.message = sb == null ? null : sb.toString();
			}

		}
	}
	
}
