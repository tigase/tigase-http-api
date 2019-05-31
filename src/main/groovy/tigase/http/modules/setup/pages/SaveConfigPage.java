package tigase.http.modules.setup.pages;

import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.questions.SingleAnswerQuestion;

import java.io.IOException;

public class SaveConfigPage extends Page {

	public SaveConfigPage(Config config) {
		super("Saving configuration", "saveConfig.html", new SingleAnswerQuestion("saveConfig", () -> "true", val -> {
			if (val != null ? (Boolean.parseBoolean(val) || "on".equals(val)) : false) {
				try {
					config.saveConfig();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		}));
	}
}
