package tigase.http.modules.setup.pages;

import tigase.db.util.SchemaManager;
import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.questions.Question;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DBCheckPage extends Page {

	private final Config config;

	public DBCheckPage(Config config) {
		super("Database connectivity check", "dbCheck.html", Stream.empty());
		this.config = config;
	}
	
	public synchronized Map<SchemaManager.DataSourceInfo, List<SchemaManager.ResultEntry>> loadSchema() {
		try {
			Map<String, Object> configStr = config.getConfigurationInMap();

			SchemaManager schemaManager = new SchemaManager();
			schemaManager.setConfig(configStr);
			schemaManager.setDbRootCredentials(config.dbProperties.getProperty("rootUser"),
											   config.dbProperties.getProperty("rootPass"));
			if (config.admins != null) {
				schemaManager.setAdmins(Arrays.asList(config.admins), config.adminPwd);
			}

			return schemaManager.loadSchemas();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}