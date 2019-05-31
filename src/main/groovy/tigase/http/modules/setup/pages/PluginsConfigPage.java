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
package tigase.http.modules.setup.pages;

import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.questions.Question;
import tigase.http.modules.setup.questions.SingleAnswerQuestion;
import tigase.server.xmppsession.SessionManager;
import tigase.util.setup.BeanDefinition;
import tigase.util.setup.SetupHelper;
import tigase.xmpp.XMPPImplIfc;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginsConfigPage extends Page implements AdvancedConfigPage {

	private List<PluginQuestion> plugins;

	public PluginsConfigPage(Config config) {
		super("Plugins selection", "plugins.html", Stream.empty());

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

	private static class PluginQuestion
			extends SingleAnswerQuestion {

		private static final Comparator<PluginQuestion> byBeanName = (e1, e2) -> e1.getBeanName()
				.compareTo(e2.getBeanName());
		private final Config config;
		private final BeanDefinition def;

		PluginQuestion(BeanDefinition def, Config config) {
			super(def.getName(), () -> config.plugins.contains(def.getName()) ? def.getName() : null, (val) -> {
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
}