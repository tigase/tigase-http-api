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

import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.questions.SingleAnswerQuestion;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.xmpp.jid.BareJID;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BasicConfigPage extends Page {

	public BasicConfigPage(Config config) {
		super("Basic Tigase server configuration", "basicConfig.html",
			  new SingleAnswerQuestion("configType", true, () -> config.getConfigType().id(),
									   type -> config.setConfigType(ConfigTypeEnum.valueForId(type))),
			  new VirtualDomainQuestion("virtualDomain", config), new AdminsQuestion("admins", config),
			  new SingleAnswerQuestion("adminPwd", () -> config.adminPwd, pwd -> config.adminPwd = pwd),
			  new SingleAnswerQuestion("dbType", true, () -> config.getDbType(), type -> config.setDbType(type)),
			  new SingleAnswerQuestion("advancedConfig", () -> String.valueOf(config.advancedConfig),
									   val -> config.advancedConfig =
											   val != null ? (Boolean.parseBoolean(val) ||
													   "on".equals(val)) : false));
	}

	private static class VirtualDomainQuestion
			extends SingleAnswerQuestion {

		VirtualDomainQuestion(String id, Config config) {
			super(id, true, () -> Optional.ofNullable(config.defaultVirtualDomain).orElse(""), vhost -> {
				config.defaultVirtualDomain = vhost;
			});
		}
	}

	private static class AdminsQuestion
			extends SingleAnswerQuestion {

		AdminsQuestion(String id, Config config) {
			super(id, false, () -> Arrays.stream(config.admins).map(jid -> jid.toString()).collect(Collectors.joining(",")),
				  admins -> {
					  if (admins != null && !admins.trim().isEmpty()) {
						  config.admins = Stream.of(admins.split(","))
								  .map(str -> str.trim())
								  .map(str -> BareJID.bareJIDInstanceNS(str))
								  .toArray(BareJID[]::new);
					  } else {
						  config.admins = new BareJID[0];
					  }
				  });
		}
	}
}
