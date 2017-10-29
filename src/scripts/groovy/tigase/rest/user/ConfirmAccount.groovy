/*
 * ConfirmAccount.groovy
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

package tigase.rest.user

import tigase.http.rest.Service
import tigase.kernel.beans.Bean
import tigase.kernel.beans.Inject
import tigase.xmpp.impl.JabberIqRegister
import tigase.xmpp.jid.BareJID

/**
 * Class implements ability to verify user account, which may be enforced during account
 * registration process.
 * Handles requests for /rest/user/confirm and executes request for currently authenticated user
 *
 * Example format of content of request or response:
 * <account><jid>user@domain</jid><status>active</status></account>*/
@Bean(name = "account-confirm", active = true)
class ConfirmAccountHandler
		extends tigase.http.rest.Handler {

	@Inject(nullAllowed = true)
	private JabberIqRegister.AccountValidator[] validators;

	public ConfirmAccountHandler() {
		description = [ regex: "/",
						GET  : [ info       : 'Retrieve details of active user account',
								 description: """Only required parameter is part of url {user_jid} which is jid of user which account informations you want to retrieve.
Data will be returned in form of JSON or XML depending on selected format by Accept HTTP header\n\

Example response:
\${util.formatData([account:[jid:'user@example.com', status:'active' ]])}				
""" ] ];
		regex = /\/confirm\/([^@\/]+)/
		isAsync = false
		apiKey = false;
		execGet = { Service service, callback, String token ->
			if (validators == null) {
				callback(
						[ error: [ message: "Account confirmation is not enabled. Please contact server administrator." ] ]);
				return;
			}
			BareJID validatedAccount = null;
			Exception exception = null;
			validators.each { validator ->
				try {
					validatedAccount = validator.validateAccount(token);
				} catch (RuntimeException ex) {
					exception = ex;
				}
			}

			if (validatedAccount != null) {
				callback([ account: [ jid: "${validatedAccount.toString()}", status: 'active' ] ]);
			} else {
				callback([ error: [ message: exception?.getMessage() ] ]);
			}
		}
	}
}
