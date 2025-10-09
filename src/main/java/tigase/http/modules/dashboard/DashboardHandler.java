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
package tigase.http.modules.dashboard;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import jakarta.ws.rs.core.SecurityContext;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;
import tigase.http.jaxrs.Handler;
import tigase.http.util.TemplateUtils;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.Base64;
import tigase.xmpp.impl.CaptchaProvider;
import tigase.xmpp.jid.BareJID;

import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public abstract class DashboardHandler implements Handler {

	public static boolean canAccess(SecurityContext securityContext, Class<? extends DashboardHandler> clazz, String methodName) {
		Method method = Arrays.stream(clazz.getDeclaredMethods())
				.filter(it -> it.getName().equals(methodName))
				.findFirst()
				.orElseThrow();
		var allowedRoles = Handler.getAllowedRoles(method);
		if (allowedRoles != null) {
			return allowedRoles.stream().anyMatch(securityContext::isUserInRole);
		}
		return true;
	}

	public static DashboardHandler getHandler() {
		return HANDLER.get();
	}

	private static final ThreadLocal<DashboardHandler> HANDLER = new ThreadLocal<>();
	private static final String CAPTCHA_SECRET_KEY = "captchaSecretKey";
	
	@Inject
	private DashboardModule module;
	@Inject
	private UserRepository userRepository_;
	protected TemplateEngine engine;
	@ConfigField(desc = "Path to template files", alias = "templatesPath")
	private String templatesPath;
	private final Random random = new Random();
	private SecretKeySpec secretKey;

	DashboardHandler() {
		setTemplatesPath(null);
	}

	public String getTemplatesPath() {
		return templatesPath;
	}

	public DashboardModule.CustomAssets getCustomAssets() {
		return module.getCustomAssets();
	}

	public void setTemplatesPath(String templatesPath) {
		this.templatesPath = templatesPath;
		this.engine = TemplateUtils.create(templatesPath, "tigase.dashboard", ContentType.Html);
	}

	protected String renderTemplate(String templateFile, Map<String, Object> model) {
		try {
			HANDLER.set(this);
			StringOutput output = new StringOutput();
			engine.render(templateFile, model, output);
			return output.toString();
		} finally {
			HANDLER.remove();
		}
	}

	protected CaptchaProvider.SimpleTextCaptcha generateCaptcha() {
		return new CaptchaProvider.SimpleTextCaptcha(random, this::getSecret);
	}

	protected boolean validateCaptcha(String captchaID, String captchaResponse) {
		if (captchaResponse == null || captchaResponse.isEmpty() || captchaID == null || captchaID.isEmpty()) {
			return false;
		}
		String[] parts = captchaID.split("\\.");
		String type = parts[0];
		if (!"simple-text".equals(type)) {
			return false;
		}

		if (!new CaptchaProvider.SimpleTextCaptcha(parts).isResponseValid(this::getSecret, captchaResponse)) {
			return false;
		}
		return true;
	}

	private SecretKeySpec getSecret() {
		if (secretKey == null) {
			try {
				BareJID user = BareJID.bareJIDInstanceNS(module.getComponentName());
				try {
					if (!userRepository_.userExists(user)) {
						userRepository_.addUser(user);
					}
				} catch (UserExistsException e) {
				}
				String secretKeyStr = userRepository_.getData(user, CAPTCHA_SECRET_KEY);
				if (secretKeyStr == null) {
					SecureRandom random = new SecureRandom();
					byte[] secret = new byte[32];
					random.nextBytes(secret);
					String newSecretKeyStr = Base64.encode(secret);
					secretKeyStr = userRepository_.getData(user, CAPTCHA_SECRET_KEY);
					if (secretKeyStr == null) {
						userRepository_.setData(user, CAPTCHA_SECRET_KEY, newSecretKeyStr);
						Thread.sleep(500);
						secretKeyStr = userRepository_.getData(user, CAPTCHA_SECRET_KEY);
					}
				}
				secretKey = new SecretKeySpec(Base64.decode(secretKeyStr), "HmacSHA256");
			} catch (Throwable ex) {
				throw new RuntimeException(ex);
			}
		}
		return secretKey;
	}
}
