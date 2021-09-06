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

import tigase.db.util.DBSchemaLoader;
import tigase.db.util.SchemaLoader;
import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.questions.Question;
import tigase.http.modules.setup.questions.SingleAnswerQuestion;
import tigase.util.ui.console.CommandlineParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DBSetupPage extends Page {

	private final Config config;
	private List<Question> questions = new ArrayList<>();

	public DBSetupPage(Config config) {
		super("Database configuration", "dbSetup.html");
		this.config = config;
	}

	@Override
	public void beforeDisplay() {
		List<CommandlineParameter> options = SchemaLoader.newInstance(config.getDbType()).getSetupOptions();
		Stream<Question> questions = options.stream()
				.filter(param -> !DBSchemaLoader.PARAMETERS_ENUM.ROOT_ASK.getName().equals(param.getFullName().orElse(null)))
				.map(o -> {
			SingleAnswerQuestion question = null;
			if (Boolean.class.equals(o.getType())) {
				question = new SingleAnswerQuestion(o.getFullName().get(), o.getDescription().get(), () -> {
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
				question = new SingleAnswerQuestion(o.getFullName().get(), o.getDescription().get(), o.isRequired(), () -> {
					String val = config.dbProperties.getProperty(o.getFullName().get());
					if (val == null) {
						val = o.getDefaultValue().orElse(null);
					}
					return val;
				}, val -> {
					if (val == null || val.trim().isEmpty()) {
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

	public List<Question> getQuestions() {
		return questions;
	}

	@Override
	protected void addQuestion(Question question) {
		super.addQuestion(question);
		questions.add(question);
	}
}