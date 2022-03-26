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
package tigase.http.modules.rest.handlers;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import gg.jte.resolve.ResourceCodeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tigase.auth.PasswordResetterIfc;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.Algorithms;
import tigase.util.Token;
import tigase.xmpp.jid.BareJID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "userResetPassword", parent = RestModule.class, active = true)
@Path("/user/resetPassword")
public class UserResetPasswordHandler
		extends AbstractRestHandler {

	private static final Logger log = Logger.getLogger(UserResetPasswordHandler.class.getCanonicalName());

	private final TemplateEngine templateEngine;

	@Inject
	private UserRepository userRepository;
	
	@Inject(nullAllowed = true)
	private PasswordResetterIfc[] resetters;

	@ConfigField(desc = "Require captcha")
	private boolean captchaRequired = true;

	public UserResetPasswordHandler() {
		super(Security.None, Role.None);
		templateEngine = TemplateEngine.create(new ResourceCodeResolver("tigase/rest"), ContentType.Html);
	}

	@GET
	@Path("/")
	@Produces({"text/html"})
	@Operation(summary = "Initial form for password reset", description = "Get initial form for account password reset")
	public Response resetPasswordSendMailForm(HttpServletRequest request) throws NoSuchAlgorithmException {
		Map<String, Object> context = new HashMap<>();
		context.put("errors", Collections.emptyList());
		if (captchaRequired) {
			context.put("captcha", Captcha.generate());
		}

		return renderResponse("resetPasswordSendMail.jte", context, request);
	}

	@POST
	@Path("/")
	@Produces({"text/html"})
	@Operation(summary = "Submit form for password reset", description = "Submit initial form for account password reset")
	public Response resetPasswordSendMail(
			@Parameter(description = "BareJID of the account") @FormParam("jid") @NotNull BareJID jid,
			@Parameter(description = "Email provided during account registration") @FormParam("email") @NotNull String email,
			@Parameter(description = "Captcha query") @FormParam("captcha-query") String captchaQuery,
			@Parameter(description = "Captcha query id") @FormParam("id") String captchaId,
			@Parameter(description = "Captcha response") @FormParam("captcha") String captchaResult,
			@HeaderParam("X-Forwarded-For") String forwarded, HttpServletRequest request)
			throws NoSuchAlgorithmException {
		List<String> errors = new ArrayList<>();

		if (jid.getLocalpart() == null) {
			errors.add("Please provide full account JID with domain part.");
		}

		if (email.trim().isEmpty()) {
			errors.add("Email address cannot be empty.");
		}

		Captcha captcha = null;
		if (captchaRequired) {
			captcha = new Captcha(captchaId, captchaQuery);
			if (!captcha.validate(captchaResult)) {
				errors.add("Provided captcha result is invalid!");
			}
		}

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
			log.log(Level.WARNING, "Database issue when processing request: " + request, ex);
			errors.add("Internal error occurred. Please try again later.");
		}

		if (errors.isEmpty()) {
			try {
				String resetUrl = Optional.ofNullable(forwardedForDomain(forwarded, request))
						.orElseGet(() -> request.getRequestURL().toString());
				sendToken(jid, resetUrl);
			} catch (Exception ex) {
				log.log(Level.WARNING, "Issue when processing request: ${request}", ex);
				errors.add("Internal error occurred. Please try again later.");
			}
		}

		Map<String, Object> context = new HashMap<>();
		context.put("jid", jid);
		context.put("email", email);
		context.put("errors", errors);
		context.put("captcha", captcha);
		return renderResponse("resetPasswordSendMail.jte", context, request);
	}

	@GET
	@Path("/{token}")
	@Produces({"text/html"})
	@Operation(summary = "Password reset form", description = "Get form to provide password for account password reset")
	public Response resetPasswordForm(
			@Parameter(description = "Token for password reset") @NotNull @PathParam("token") String token,
			HttpServletRequest request) {
		Optional<PasswordResetterIfc> resetter = findResetter(token);

		Map<String, Object> context = new HashMap<>();
		if (resetter.isPresent()) {
			BareJID jid = Token.parse(token).getJid();
			context.put("jid", jid);
		} else {
			context.put("error", "This link is not valid");
		}

		return renderResponse("resetPasswordForm.jte", context, request);
	}

	@POST
	@Path("/{token}")
	@Produces({"text/html"})
	@Operation(summary = "Submit password reset form", description = "Submit form to provide password for account password reset")
	public Response resetPassword(
			@Parameter(description = "Token for password reset") @NotNull @PathParam("token") String token,
			@Parameter(description = "New password") @NotNull @FormParam("password-entry") String p1,
			@Parameter(description = "New password confirmation") @NotNull @FormParam("password-reentry") String p2,
			HttpServletRequest request) {
		Optional<PasswordResetterIfc> resetter = findResetter(token);

		Map<String, Object> context = new HashMap<>();
		if (resetter.isPresent()) {
			BareJID jid = Token.parse(token).getJid();
			if (p1 != null && p2 != null && p1.equals(p2)) {
				try {
					resetter.get().changePassword(token, p1);
					context.put("jid", jid);
				} catch (Exception ex) {
					context.put("error", "Internal error occurred. Please try again later.");
				}
			}
		} else {
			context.put("error", "This link is not valid");
		}
		return renderResponse("resetPassword.jte", context, request);
	}

	private Response renderResponse(String templateName, Map<String, Object> context, HttpServletRequest request) {
		StringOutput output = new StringOutput();
		context.put("basePath", request.getContextPath());
		templateEngine.render(templateName, context, output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	private String forwardedForDomain(String forwarded, HttpServletRequest request) {
		if (forwarded == null) {
			return null;
		}

		String address = request.getRemoteAddr();
		String clearAddress = address.replaceAll("/", "");
		if (clearAddress == "127.0.0.1") {
			return  forwarded.split(",")[0];
		}

		return null;
	}

	private void sendToken(BareJID jid, String url) throws Exception {
		for (PasswordResetterIfc resetter : resetters) {
			resetter.sendToken(jid, url.endsWith("/") ? url : (url + "/"));
		}
	}

	private Optional<PasswordResetterIfc> findResetter(String token) {
		return Arrays.stream(resetters).filter(it -> {
			try {
				it.validateToken(token);
				return true;
			} catch (Exception ex) {
				return false;
			}
		}).findFirst();
	}

	public static class Captcha {

		private static Random random = new Random();

		public static Captcha generate() throws NoSuchAlgorithmException {
			int x = random.nextInt(31) + 1;
			int y = random.nextInt(31) + 1;

			if (random.nextBoolean()) {
				int result = x + y;
				String captcha = "$x + $y";
				String id = Algorithms.bytesToHex(
						MessageDigest.getInstance("SHA1").digest((captcha + " = " + result).getBytes(StandardCharsets.UTF_8)));
				return new Captcha(id, captcha);
			} else {
				if (y > x) {
					int t = x;
					x = y;
					y = t;
				}
				int result = x - y;
				String captcha = "$x - $y";
				String id = Algorithms.bytesToHex(
						MessageDigest.getInstance("SHA1").digest((captcha + " = " + result).getBytes(StandardCharsets.UTF_8)));
				return new Captcha(id, captcha);
			}
		}

		private final String id;
		private final String query;

		public Captcha(String id, String query) {
			this.id = id;
			this.query = query;
		}

		public String getId() {
			return id;
		}

		public String getQuery() {
			return query;
		}

		public boolean validate(String result) throws NoSuchAlgorithmException {
			return Algorithms.bytesToHex(
					MessageDigest.getInstance("SHA1").digest((query + " = " + result.trim()).getBytes(StandardCharsets.UTF_8))).
					equals(id);
		}
	}
}
