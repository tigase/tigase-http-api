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
import tigase.db.util.SchemaManager;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.server.BasicComponent;
import tigase.server.xmppsession.SessionManager;
import tigase.util.setup.BeanDefinition;
import tigase.util.setup.SetupHelper;
import tigase.util.ui.console.CommandlineParameter;
import tigase.xmpp.XMPPImplIfc;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
						   new SingleAnswerQuestion("dbType", ()-> config.getDbType(), type -> config.setDbType(type)),
						   new SingleAnswerQuestion("advancedConfig", () -> String.valueOf(config.advancedConfig), val -> config.advancedConfig = val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false)
						   ));
		pages.add(new AdvConfigPage("Advanced configuration options", config, Stream.of(
				new SingleAnswerQuestion("clusterMode", () -> String.valueOf(config.clusterMode), val -> config.clusterMode = val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false),
				new SingleAnswerQuestion("acsComponent", () -> String.valueOf(config.acs), val -> config.acs = val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false)
		)));

		pages.add(new PluginsConfigPage("Plugins selection", config));

		pages.add(new DBSetupPage("Database configuration"));

		pages.add(new DBCheckPage("Database connectivity check"));

		pages.add(new Page("HTTP API - REST security configuration",
				  new SingleAnswerQuestion("httpRestApiSecurity", ()-> config.httpSecurity.restApiSecurity.name(),
										   val -> config.httpSecurity.restApiSecurity = SetupHelper.RestApiSecurity.valueOf(val)),
				  new SingleAnswerQuestion("httpRestApiKeys", ()-> Arrays.stream(config.httpSecurity.restApiKeys).collect(Collectors.joining(",")),
										   (val)-> config.httpSecurity.restApiKeys = val == null ?  new String[0] : val.split(","))));

		pages.add(new Page("Setup security",
						   new SingleAnswerQuestion("setupUser", ()-> config.httpSecurity.setupUser, user -> config.httpSecurity.setupUser = user),
						   new SingleAnswerQuestion("setupPassword", ()-> config.httpSecurity.setupPassword, pass -> config.httpSecurity.setupPassword = pass)));

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
			setQuestions(questions);
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

		public void beforeDisplay() {
			
		}

		protected void setQuestions(Stream<Question> questions) {
			this.questions.clear();
			questions.forEach(this::addQuestion);
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
		private boolean secret = false;

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

		public boolean isSecret() {
			return secret;
		}

		protected void setSecret(boolean secret) {
			this.secret = secret;
		}
	}

	public static class SingleAnswerQuestion extends Question {

		private final String label;
		private final Supplier<String> getter;
		private final Consumer<String> setter;

		public SingleAnswerQuestion(String id, Supplier<String> getter, Consumer<String> setter) {
			this(id, null, getter, setter);
		}

		public SingleAnswerQuestion(String id, String label, Supplier<String> getter, Consumer<String> setter) {
			super(id);
			this.label = label;
			this.getter = getter;
			this.setter = setter;
		}

		public String getLabel() {
			return label;
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

	private class DBSetupPage extends Page {

		private List<Question> questions = new ArrayList<>();

		public DBSetupPage(String title) {
			super(title);
		}

		@Override
		public void beforeDisplay() {
			List<CommandlineParameter> options = SchemaLoader.newInstance(config.getDbType())
					.getSetupOptions();
			Stream<Question> questions = options.stream().map(o -> {
				SingleAnswerQuestion question = null;
				if (Boolean.class.equals(o.getType())) {
					question = new SingleAnswerQuestion(o.getFullName().get(), o.getDescription().get(), ()-> {
						String val = config.dbProperties.getProperty(o.getFullName().get());
						if (val == null) {
							val = o.getDefaultValue().orElse(null);
						}
						return val;
					}, val -> {
						boolean bval = val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false;
						config.dbProperties.setProperty(o.getFullName().get(), String.valueOf(bval));
					});
				} else {
					question = new SingleAnswerQuestion(o.getFullName().get(), o.getDescription().get(), ()-> {
						String val = config.dbProperties.getProperty(o.getFullName().get());
						if (val == null) {
							val = o.getDefaultValue().orElse(null);
						}
						return val;
					}, val -> {
						if (val == null) {
							config.dbProperties.remove(o.getFullName().get());
						} else {
							config.dbProperties.setProperty(o.getFullName().get(), val);
						}
					});
					question.setSecret(o.isSecret());
				}
				return question;
			});
			this.questions.clear();
			setQuestions(questions);
		}

		@Override
		protected void addQuestion(Question question) {
			super.addQuestion(question);
			questions.add(question);
		}

		public List<Question> getQuestions() {
			return questions;
		}
	}

	private class DBCheckPage extends Page {

		public DBCheckPage(String title, Stream<Question> questions) {
			super(title, questions);
		}

		public DBCheckPage(String title, Question... questions) {
			super(title, questions);
		}

		public synchronized Map<SchemaManager.DataSourceInfo, List<SchemaManager.ResultEntry>> loadSchema() {
			try {
				Map<String, Object> configStr = config.getConfigurationInMap();

				SchemaManager schemaManager = new SchemaManager();
				schemaManager.setConfig(configStr);
				schemaManager.setDbRootCredentials(config.dbProperties.getProperty("rootUser"), config.dbProperties.getProperty("rootPass"));

				return schemaManager.loadSchemas();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}
	
}
