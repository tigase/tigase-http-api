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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConnectorsPage
		extends Page implements SimpleConfigPage {

	private final Config config;

	private List<SingleAnswerQuestion> questionsWithLabels = new ArrayList<>();

	public ConnectorsPage(Config config) {
		super("Connectivity", "connectors.html", Stream.empty());
		this.config = config;
		addConnectorsQuestions();
	}

	@Override
	protected void addQuestion(Question question) {
		super.addQuestion(question);
		if (question instanceof SingleAnswerQuestion) {
			questionsWithLabels.add((SingleAnswerQuestion) question);
		}
	}

	public List<SingleAnswerQuestion> getQuestionsWithLabels() {
		return questionsWithLabels;
	}

	private void addConnectorsQuestions() {
		SingleAnswerQuestion question = new ConnectorsQuestion("c2s", "Desktop / Mobile", config);
		addQuestion(question);
		question = new ConnectorsQuestion("bosh", "Web (HTTP)", config);
		addQuestion(question);
		question = new ConnectorsQuestion("ws2s", "Web (WebSocket)", config);
		addQuestion(question);
		question = new ConnectorsQuestion("s2s", "Federation", config);
		addQuestion(question);
		question = new SingleAnswerQuestion("ext", "External", ()-> (config.optionalComponents.contains("ext") && config.optionalComponents.contains("ext-man")) ? "ext" : null, (val) -> {
			if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
				config.optionalComponents.add("ext");
				config.optionalComponents.add("ext-man");
			} else {
				config.optionalComponents.remove("ext");
				config.optionalComponents.remove("ext-man");
			}
		});
		addQuestion(question);
		question = new SingleAnswerQuestion("http", "REST API/Admin UI", () -> config.optionalComponents.contains("http") ? "http" :null, (val) -> {
			if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
				config.optionalComponents.add("http");
			} else {
				config.optionalComponents.remove("http");
			}
		});
		addQuestion(question);
	}

	public static class ConnectorsQuestion extends SingleAnswerQuestion {

		public ConnectorsQuestion(String id, String label, Config config) {
			super(id, label, ()-> config.optionalComponents.contains(id) ? id : null, (val) -> {
				if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
					config.optionalComponents.add(id);
				} else {
					config.optionalComponents.remove(id);
				}
			});
		}

		
	}
}
