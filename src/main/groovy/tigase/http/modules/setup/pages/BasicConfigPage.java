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
			  new SingleAnswerQuestion("configType", () -> config.getConfigType().id(),
									   type -> config.setConfigType(ConfigTypeEnum.valueForId(type))),
			  new VirtualDomainQuestion("virtualDomain", config), new AdminsQuestion("admins", config),
			  new SingleAnswerQuestion("adminPwd", () -> config.adminPwd, pwd -> config.adminPwd = pwd),
			  new SingleAnswerQuestion("dbType", () -> config.getDbType(), type -> config.setDbType(type)),
			  new SingleAnswerQuestion("advancedConfig", () -> String.valueOf(config.advancedConfig),
									   val -> config.advancedConfig =
											   val != null ? (Boolean.parseBoolean(val) ||
													   "on".equals(val)) : false));
	}

	private static class VirtualDomainQuestion
			extends SingleAnswerQuestion {

		VirtualDomainQuestion(String id, Config config) {
			super(id, () -> Optional.ofNullable(config.defaultVirtualDomain).orElse(""), vhost -> {
				config.defaultVirtualDomain = vhost;
			});
		}
	}

	private static class AdminsQuestion
			extends SingleAnswerQuestion {

		AdminsQuestion(String id, Config config) {
			super(id, () -> Arrays.stream(config.admins).map(jid -> jid.toString()).collect(Collectors.joining(",")),
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
