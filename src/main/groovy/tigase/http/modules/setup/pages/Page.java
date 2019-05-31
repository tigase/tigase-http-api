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

import tigase.http.modules.setup.Setup;
import tigase.http.modules.setup.questions.Question;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class Page {

	private final Map<String, Question> questions = new ConcurrentHashMap<>();
	private final String title;
	private final String template;

	private Setup setup;

	public Page(String title, String template, Stream<Question> questions) {
		this.title = title;
		this.template = template;
		setQuestions(questions);
	}

	public Page(String title, String template, Question... questions) {
		this(title, template, Arrays.stream(questions));
	}

	public String getId() {
		return String.valueOf(setup.pageId(this));
	}

	public void init(Setup setup) {
		this.setup = setup;
	}

	public String getTemplate() {
		return template;
	}

	public String getTitle() {
		return title;
	}

	public Question getQuestion(String id) {
		return questions.get(id);
	}

	public void beforeDisplay() {

	}

	public String nextPage() {
		return String.valueOf(setup.nextPageId(this));
	}

	protected void addQuestion(Question question) {
		question.setPage(this);
		this.questions.put(question.getId(), question);
	}

	protected void setQuestions(Stream<Question> questions) {
		this.questions.clear();
		questions.forEach(this::addQuestion);
	}

	public void setValues(Map<String, String[]> params) {
		questions.values().forEach(question -> {
			String[] value = params.get(question.getName());
			question.setValues(value);
		});
	}

	public boolean isValid() {
		return !questions.values().stream().filter(question -> !question.isValid()).findFirst().isPresent();
	}
}
