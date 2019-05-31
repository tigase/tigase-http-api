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
package tigase.http.modules.setup;

import tigase.http.modules.setup.pages.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 30.03.2017.
 */
public class Setup {

	private final Config config = new Config();
	private final List<Page> pages = new ArrayList<>();

	public Setup() {
		addPage(new AboutSoftwarePage());
		addPage(new ACSInfoPage(config));

		addPage(new BasicConfigPage(config));

		addPage(new AdvConfigPage(config));

		addPage(new PluginsConfigPage(config));

		addPage(new DBSetupPage(config));

		addPage(new DBCheckPage(config));

		addPage(new SetupSecurityPage(config));

		addPage(new SaveConfigPage(config));
		addPage(new Page("Finished", "finished.html"));
	}

	private void addPage(Page page) {
		page.init(this);
		pages.add(page);
	}

	public Page getPageById(int page) {
		return pages.get(page -1);
	}

	public int pageId(Page page) {
		if (page == null) {
			return 1;
		}
		return this.pages.indexOf(page) + 1;
	}

	public int nextPageId(Page page) {
		if (page == null) {
			return 1;
		}
		return this.pages.indexOf(page) + 2;
	}

	public List<String> getTemplates() {
		return pages.stream().map(Page::getTemplate).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
}
