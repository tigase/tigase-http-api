package tigase.http.modules.setup.pages;

import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.questions.SingleAnswerQuestion;

public class ACSInfoPage extends Page {

	private final Config config;

	public ACSInfoPage(Config config) {
		super("Advanced Clustering Strategy information", "acsInfo.html",
				new SingleAnswerQuestion("acsName", config::getAcsName, config::setAcsName));
		this.config = config;
	}

}
