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
