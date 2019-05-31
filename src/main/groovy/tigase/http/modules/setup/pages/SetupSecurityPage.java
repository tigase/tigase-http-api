package tigase.http.modules.setup.pages;

import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.questions.SingleAnswerQuestion;

public class SetupSecurityPage extends Page {

	public SetupSecurityPage(Config config) {
		super("Setup security", "setupSecurity.html", new SingleAnswerQuestion("setupUser", () -> config.httpSecurity.setupUser,
																			   user -> config.httpSecurity.setupUser = user),
			  new SingleAnswerQuestion("setupPassword", () -> config.httpSecurity.setupPassword,
									   pass -> config.httpSecurity.setupPassword = pass));
	}
}
