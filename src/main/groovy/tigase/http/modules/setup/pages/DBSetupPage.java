package tigase.http.modules.setup.pages;

import tigase.db.util.SchemaLoader;
import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.pages.Page;
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
		Stream<Question> questions = options.stream().map(o -> {
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
				question = new SingleAnswerQuestion(o.getFullName().get(), o.getDescription().get(), () -> {
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