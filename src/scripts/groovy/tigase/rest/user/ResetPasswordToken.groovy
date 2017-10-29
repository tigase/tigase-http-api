/*
 * ResetPasswordToken.groovy
 *
 * Tigase HTTP API
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
import tigase.http.rest.Service
import tigase.kernel.beans.Bean
import tigase.kernel.beans.Inject
import tigase.util.Token
import tigase.xmpp.jid.BareJID

import javax.servlet.http.HttpServletRequest

@Bean(name = "password-reset-token", active = true)
class ResetPasswordTokenHandler
		extends tigase.http.rest.Handler {

	@Inject(nullAllowed = true)
	private PasswordResetterIfc[] resetters;

	public ResetPasswordTokenHandler() {
		description = [ regex: "/",
						GET  : [ info       : 'Reset password for an account',
								 description: """""" ] ];
		regex = /\/resetPassword\/([^\/]+)/
		isAsync = false;
		apiKey = false;
		execGet = { Service service, callback, String token ->
			if (resetters == null) {
				callback([ error: "Password resetting is disabled. Please contact server administrator." ])
				return;
			}

			PasswordResetterIfc resetter = resetters.find({ it ->
				try {
					it.validateToken(token);
					return true;
				} catch (Exception ex) {
					return false;
				}
			});

			if (resetter == null) {
				callback([ error: "This link is not valid" ]);
				return;
			}

			BareJID jid = Token.parse(token).getJid();
			callback([ jid: jid, action: 'form' ]);
		}

		execPost = { Service service, callback, HttpServletRequest request, String token ->
			if (resetters == null) {
				callback([ error: "Password resetting is disabled. Please contact server administrator." ])
				return;
			}

			BareJID jid = Token.parse(token).getJid();
			String p1 = request.getParameter("password-entry");
			String p2 = request.getParameter("password-reentry");

			PasswordResetterIfc resetter = resetters.find({ it ->
				try {
					it.validateToken(token);
					return true;
				} catch (Exception ex) {
					return false;
				}
			});

			def errors = [ ];
			def action = 'form';

			if (resetter == null) {
				callback([ error: "This link is not valid" ]);
				return;
			} else {
				if (p1 != null && p2 != null && p1.equals(p2)) {
					try {
						resetter.changePassword(token, p1);
						action = 'success';
					} catch (Exception ex) {
						errors.add("Internal error occurred. Please try again later.");
					}
				}
			}

			callback([ jid: jid, action: action, errors: errors ]);
		}
	}

}
