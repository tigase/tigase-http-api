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
package tigase.http.modules.setup.pages;

import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.questions.Question;
import tigase.http.modules.setup.questions.SingleAnswerQuestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FeaturesPage extends Page implements SimpleConfigPage {

	private final Config config;

	private final List<SingleAnswerQuestion> clusteringQuestions = new ArrayList<>();

	private final List<SingleAnswerQuestion> featureQuestions = new ArrayList<>();

	public FeaturesPage(Config config) {
		super("Features", "features.html", Stream.empty());
		this.config = config;

		addClusteringQuestions();
		addFeatureQuestions();
	}

	public List<SingleAnswerQuestion> getClusteringQuestions() {
		return clusteringQuestions;
	}

	public List<SingleAnswerQuestion> getFeatureQuestions() {
		return featureQuestions;
	}

	@Override
	public void setValues(Map<String, String[]> params) {
		super.setValues(params);

		if (config.getClusterMode() && (config.optionalComponents.contains("muc") || config.optionalComponents.contains("pubsub"))) {
			config.setACS(true);
		}
	}

	@Override
	protected void addQuestion(Question question) {
		super.addQuestion(question);
		if (question instanceof ClusteringQuestion) {
			clusteringQuestions.add((ClusteringQuestion) question);
		}
		if (question instanceof FeatureQuestion) {
			featureQuestions.add((FeatureQuestion) question);
		}
	}

	private void addClusteringQuestions() {
		SingleAnswerQuestion question = new ClusteringQuestion("clusterMode", "Do you want your server to run in the cluster mode?", () -> String.valueOf(config.getClusterMode()),
								 val -> config.setClusterMode(
										 val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false));
		addQuestion(question);
		question = new ClusteringQuestion("acsComponent", "Tigase Advanced Clustering Strategy (ACS)", () -> String.valueOf(config.getACS()), val -> config.setACS(
						val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false));
		addQuestion(question);
	}

	private void addFeatureQuestions() {
		SingleAnswerQuestion question = new ComponentFeatureQuestion("muc", "Multi User Chat", config);
		addQuestion(question);
		question = new ComponentAndProcessorsFeatureQuestion("pubsub", "Publish-Subscribe", new String[]{"pep"}, config,
															 null);
		addQuestion(question);
		question = new ComponentAndProcessorsFeatureQuestion("message-archive", "Message Archive",
															 new String[]{"message-archive-xep-0136",
																		  "urn:xmpp:mam:1"}, config,
															 () -> config.optionalComponents.remove("unified-archive"));
		addQuestion(question);
		question = new ProcessorFeatureQuestion("urn:xmpp:push:0", "PUSH Notifications", config);
		addQuestion(question);
		question = new ComponentFeatureQuestion("upload", "HTTP File Upload", config);
		addQuestion(question);
		question = new ProcessorFeatureQuestion("message-carbons", "Message Carbons", config);
		addQuestion(question);
		question = new ProcessorFeatureQuestion("urn:xmpp:csi:0", "Client State Indication", config);
		addQuestion(question);
		question = new ProcessorFeatureQuestion("spam-filter", "SPAM Filter", config);
		addQuestion(question);
	}

	private static class ClusteringQuestion extends SingleAnswerQuestion {

		public ClusteringQuestion(String id, String label, Supplier<String> getter, Consumer<String> setter) {
			super(id, label, getter, setter);
		}
	}

	private static class FeatureQuestion extends SingleAnswerQuestion {

		public FeatureQuestion(String id, String label, Supplier<String> getter, Consumer<String> setter) {
			super(id, label, getter, setter);
		}
	}

	private static class ComponentFeatureQuestion extends FeatureQuestion {

		public ComponentFeatureQuestion(String id, String label, Config config) {
			super(id, label, ()-> config.optionalComponents.contains(id) ? id : null, (val) -> {
				if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
					config.optionalComponents.add(id);
				} else {
					config.optionalComponents.remove(id);
				}
			});
		}
	}

	private static class ComponentAndProcessorsFeatureQuestion extends FeatureQuestion {

		public ComponentAndProcessorsFeatureQuestion(String id, String label, String[] processorIds, Config config, Runnable changeListener) {
			super(id, label, ()-> (config.optionalComponents.contains(id) && config.plugins.containsAll(Arrays.asList(processorIds))) ? id : null, (val) -> {
				if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
					config.optionalComponents.add(id);
					config.plugins.addAll(Arrays.asList(processorIds));
				} else {
					config.optionalComponents.remove(id);
					config.plugins.removeAll(Arrays.asList(processorIds));
				}
				if (changeListener != null) {
					changeListener.run();
				}
			});
		}
	}

	private static class ProcessorFeatureQuestion extends FeatureQuestion {

		public ProcessorFeatureQuestion(String id, String label, Config config) {
			super(id, label, ()-> (config.plugins.contains(id)) ? id : null, (val) -> {
				if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
					config.plugins.add(id);
				} else {
					config.plugins.remove(id);
				}
			});
		}
	}
}
