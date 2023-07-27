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
package rest.user

import tigase.auth.PasswordResetterIfc
import tigase.db.TigaseDBException
import tigase.db.UserNotFoundException
import tigase.db.UserRepository
import tigase.http.rest.Service
import tigase.kernel.beans.Bean
import tigase.kernel.beans.Inject
import tigase.kernel.beans.config.ConfigField
import tigase.util.Algorithms
import tigase.util.stringprep.TigaseStringprepException
import tigase.xmpp.jid.BareJID

import javax.servlet.http.HttpServletRequest
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger

@Bean(name = "password-reset-form", active = true)
class ResetPasswordFormHandler
		extends tigase.http.rest.Handler {

	def log = Logger.getLogger("tigase.rest")

	private Random random = new Random();

	@Inject
	private UserRepository userRepository;

	@Inject(nullAllowed = true)
	private PasswordResetterIfc[] resetters;

	@ConfigField(desc = "Require captcha")
	private boolean captchaRequired = true;

	public ResetPasswordFormHandler() {
		description = [ regex: "/resetPassword/",
						GET  : [ info       : 'Reset password for an account',
								 description: """""" ] ];
		regex = /\/resetPassword/
		isAsync = false;
		apiKey = false;
		execGet = { Service service, callback ->
			if (resetters == null) {
				callback([ error: "Password resetting is disabled. Please contact server administrator." ])
				return;
			}
			callback([ captcha: generateCaptcha() ]);
		}
		execPost = { Service service, callback, HttpServletRequest request ->
			def errors = [ ];
			String jidStr = request.getParameter("jid");
			BareJID jid = null;
			if (jidStr != null && !jidStr.trim().isEmpty()) {
				try {
					jid = BareJID.bareJIDInstance(request.getParameter("jid"));
					if (jid.getLocalpart() == null) {
						errors.add("Please provide full account JID with domain part.");
					}
				} catch (TigaseStringprepException ex) {
					errors.add("Provided JID is invalid.")
				}
			} else {
				errors.add("JID cannot be empty.");
			}
			String email = request.getParameter("email");
			if (email != null && !email.trim().isEmpty()) {
			} else {
				errors.add("Email address cannot be empty.");
			}

			def captcha = null;
			if (captchaRequired) {
				String captchaQuery = request.getParameter("captcha-query") ?: "";
				String id = request.getParameter("id") ?: "";
				String result = request.getParameter("captcha") ?: "";
				if (!validateCaptcha(id, captchaQuery, result)) {
					errors.add("Provided captcha result is invalid!");
				}
				captcha = [ id: id, captcha: captchaQuery ];
			}

			try {
				String storedEmail = userRepository.getData(jid, "email");
				if (storedEmail == null || !storedEmail.equalsIgnoreCase(email)) {
					errors.add("Provided email address does not match email address used during registration. " +
									   "Please try different one or contact XMPP server administrator.")
				}
			} catch (UserNotFoundException ex) {
				errors.add("Provided email address does not match email address used during registration. " +
								   "Please try different one or contact XMPP server administrator.")
			} catch (TigaseDBException ex) {
				log.log(Level.WARNING, "Database issue when processing request: " + request, ex)
				errors.add("Internal error occurred. Please try again later.")
			}

			if (errors.isEmpty()) {
				try {
					def requestURL = request.getRequestURL().toString()
					def forwarded = getForwardedFor(request);
					def resetUrl = forwarded ? replaceRequestDomain(requestURL, forwarded) : requestURL
					sendToken(jid, resetUrl);
				} catch (Exception ex) {
					log.log(Level.WARNING, "Issue when processing request: ${request}", ex)
					errors.add("Internal error occurred. Please try again later.");
				}
			}

			callback([ jid: jidStr, email: email, errors: errors, captcha: captcha ]);
		}
	}

	void sendToken(BareJID jid, String url) {
		resetters.each {
			it.sendToken(jid, url.endsWith("/") ? url : (url + "/"));
		}
	}

	def generateCaptcha() {
		if (!captchaRequired) {
			return null;
		}

		def x = random.nextInt(31) + 1;
		def y = random.nextInt(31) + 1;

		if (random.nextBoolean()) {
			def result = x + y;
			String captcha = "$x + $y";
			String id = Algorithms.bytesToHex(
					MessageDigest.getInstance("SHA1").digest((captcha + " = " + result).getBytes("UTF-8")));
			return [ id: id, captcha: captcha ];
		} else {
			if (y > x) {
				def t = x;
				x = y;
				y = t;
			}
			def result = x - y;
			String captcha = "$x - $y";
			String id = Algorithms.bytesToHex(
					MessageDigest.getInstance("SHA1").digest((captcha + " = " + result).getBytes("UTF-8")));
			return [ id: id, captcha: captcha ];
		}
	}

	boolean validateCaptcha(String id, String captcha, String result) {
		return Algorithms.bytesToHex(
				MessageDigest.getInstance("SHA1").digest((captcha + " = " + result.trim()).getBytes("UTF-8"))).
				equals(id);
	}

	private String getForwardedFor(HttpServletRequest request) {
		def forwardedHostname = null
		try {
			def forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded) {
				def address = request.getRemoteAddr()
				def clearAddress = address.replaceAll("/", "");
				if (clearAddress == "127.0.0.1") {
					forwardedHostname = forwarded.split(",")[0]
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Error obtaining forward address", e)
		}
		return forwardedHostname;
	}

	private String replaceRequestDomain(String url, String domain) {
		final int start = url.indexOf("//")+2;
		final int stop = url.indexOf("/", start);
		return url.substring(0,start) + domain + url.substring(stop);
	}
}
