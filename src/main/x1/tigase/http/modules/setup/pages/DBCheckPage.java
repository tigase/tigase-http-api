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

import tigase.db.util.SchemaManager;
import tigase.http.modules.setup.Config;

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
			if (config.dbProperties.getProperty("rootUser") != null || config.dbProperties.getProperty("rootPass") != null) {
				schemaManager.setDbRootCredentials(config.dbProperties.getProperty("rootUser"), config.dbProperties.getProperty("rootPass"));
			}
			if (config.admins != null) {
				schemaManager.setAdmins(Arrays.asList(config.admins), config.adminPwd);
			}

			return schemaManager.loadSchemas();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}