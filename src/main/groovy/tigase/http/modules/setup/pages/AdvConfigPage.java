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
import tigase.server.BasicComponent;
import tigase.util.setup.BeanDefinition;
import tigase.util.setup.SetupHelper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdvConfigPage extends Page implements AdvancedConfigPage {

	private final Config config;
	private List<ComponentQuestion> optionalComponents = new ArrayList<>();

	public AdvConfigPage(Config config) {
		super("Advanced configuration options", "advConfig.html", Stream.of(
				new SingleAnswerQuestion("clusterMode", () -> String.valueOf(config.getClusterMode()),
										 val -> config.setClusterMode(
												 val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false)),
				new SingleAnswerQuestion("acsComponent", () -> String.valueOf(config.getACS()), val -> config.setACS(
						val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false))));
		this.config = config;

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

	@Override
	public void setValues(Map<String, String[]> params) {
		super.setValues(params);

		if (config.getClusterMode() && (config.optionalComponents.contains("muc") || config.optionalComponents.contains("pubsub"))) {
			config.setACS(true);
		}
	}

	private static class ComponentQuestion
			extends SingleAnswerQuestion {

		private static final Comparator<ComponentQuestion> byBeanName = (e1, e2) -> e1.getBeanName()
				.compareTo(e2.getBeanName());

		private final BeanDefinition def;

		ComponentQuestion(BeanDefinition def, Config config) {
			super(def.getName(), () -> config.optionalComponents.contains(def.getName()) ? def.getName() : null,
				  (val) -> {
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
}