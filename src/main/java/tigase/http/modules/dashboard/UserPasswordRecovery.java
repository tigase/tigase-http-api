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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import tigase.auth.PasswordResetterIfc;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.http.jaxrs.Model;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.Token;
import tigase.xmpp.impl.CaptchaProvider;
import tigase.xmpp.jid.BareJID;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "resetPassword", parent = DashboardModule.class, active = true)
@Path("/users/resetPassword")
public class UserPasswordRecovery extends DashboardHandler {

	private static final Logger log = Logger.getLogger(UserPasswordRecovery.class.getName());
	@ConfigField(desc = "Require captcha")
	private boolean captchaRequired = true;
	@Inject
	private UserRepository userRepository;
	@Inject(nullAllowed = true)
	private PasswordResetterIfc[] resetters;

	@Override
	public Role getRequiredRole() {
		return Role.None;
	}

	public boolean isPasswordResetEnabled()	{
		return resetters != null && resetters.length > 0;
	}

	@GET
	@Path("")
	public Response form(Model model) {
		if (captchaRequired) {
			CaptchaProvider.SimpleTextCaptcha captcha = generateCaptcha();
			model.put("captcha", captcha.getCaptchaRequest());
			model.put("captchaID", captcha.getID());
		}
		model.put("enabled", isPasswordResetEnabled());
		if (!isPasswordResetEnabled()) {
			model.put("errors", List.of("Password resetting is disabled!"));
		}
		String output = renderTemplate("users/resetPasswordForm.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	@POST
	@Path("")
	public Response executeForm(@FormParam("jid") @NotEmpty BareJID jid, @FormParam("email") @Email String email,
							@FormParam("captcha") String captchaResponse, @FormParam("captchaID") String captchaID,
							UriInfo uriInfo, Model model) throws Exception {
		List<String> errors = new ArrayList<>();
		if (captchaRequired) {
			if (!validateCaptcha(captchaID,captchaResponse)) {
				errors.add("Provided captcha result is invalid!");
			}
		}
		if (errors.isEmpty()) {
			try {
				String storedEmail = userRepository.getData(jid, "email");
				if (storedEmail == null || !storedEmail.equalsIgnoreCase(email)) {
					errors.add("Provided email address does not match email address used during registration. " +
									   "Please try different one or contact XMPP server administrator.");
				}
			} catch (UserNotFoundException ex) {
				errors.add("Provided email address does not match email address used during registration. " +
								   "Please try different one or contact XMPP server administrator.");
			} catch (TigaseDBException ex) {
				log.log(Level.WARNING, "Database issue when processing request for user " + jid, ex);
				errors.add("Internal error occurred. Please try again later.");
			}
		}

		if (!errors.isEmpty()) {
			model.put("errors", errors);
			return form(model);
		} else {
			String resetUrl = uriInfo.getBaseUriBuilder().path(UserPasswordRecovery.class).build().toString();
			if (resetters != null) {
				for (PasswordResetterIfc resetter : resetters) {
					resetter.sendToken(jid, resetUrl.endsWith("/") ? resetUrl : (resetUrl + "/"));
				}
			}
			String output = renderTemplate("users/resetPasswordForm.jte", model);
			return Response.ok(output, MediaType.TEXT_HTML).build();
		}
	}
	
	@GET
	@Path("/{token}")
	public Response resetPassword(@PathParam("token") @NotEmpty String token, Model model) {
		if (!isPasswordResetEnabled()) {
			model.put("errors", List.of("Password resetting is disabled!"));
			model.put("enabled", isPasswordResetEnabled());
			return form(model);
		}
		PasswordResetterIfc resetter = Arrays.stream(resetters).filter(it -> {
			try {
				it.validateToken(token);
				return true;
			} catch (Exception ex) {
				return false;
			}
		}).findFirst().orElse(null);

		if (resetter == null) {
			model.put("errors", List.of("Invalid token!"));
			return form(model);
		}

		try {
			BareJID jid = Token.parse(token).getJid();
			String output = renderTemplate("users/resetPasswordTokenForm.jte", model);
			return Response.ok(output, MediaType.TEXT_HTML).build();
		} catch (RuntimeException e) {
			model.put("errors", List.of(e.getMessage()));
			return form(model);
		}
	}

	@POST
	@Path("/{token}")
	public Response resetPassword(@PathParam("token") @NotEmpty String token,
								  @FormParam("password-entry") @NotEmpty String passwordEntry,
								  @FormParam("password-reentry") @NotEmpty String passwordReentry, Model model) {
		BareJID jid = Token.parse(token).getJid();
		PasswordResetterIfc resetter = Arrays.stream(resetters).filter(it -> {
			try {
				it.validateToken(token);
				return true;
			} catch (Exception ex) {
				return false;
			}
		}).findFirst().orElse(null);

		if (resetter == null) {
			model.put("errors", List.of("Invalid token!"));
			return form(model);
		} else {
			if (Objects.equals(passwordEntry, passwordReentry)) {
				try {
					resetter.changePassword(token, passwordEntry);
				} catch (Exception ex) {
					model.put("errors", List.of("Internal error occurred. Please try again later."));
					return form(model);
				}
			}
			model.put("jid", jid);
			String output = renderTemplate("users/resetPasswordToken.jte", model);
			return Response.ok(output, MediaType.TEXT_HTML).build();
		}
	}
	
}
