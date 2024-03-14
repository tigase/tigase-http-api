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
package tigase.http;

import tigase.auth.credentials.Credentials;
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.Optional;

@Bean(name = "authProvider", parent = HttpMessageReceiver.class, active = true, exportable = true)
public class AuthProviderImpl
		implements AuthProvider {

	@Inject(nullAllowed = true)
	private UserRepository userRepository;
	@Inject(nullAllowed = true)
	private AuthRepository authRepository;
	@Inject(bean = "service")
	private HttpMessageReceiver receiver;

	public AuthProviderImpl() {
	}

	@Override
	public boolean isAdmin(BareJID user) {
		return receiver.isAdmin(JID.jidInstance(user));
	}
	
	@Override
	public boolean checkCredentials(String user, final String password)
			throws TigaseStringprepException, TigaseDBException, AuthorizationException {
		if (authRepository == null) {
			return false;
		}
		BareJID jid = BareJID.bareJIDInstance(user);
		Credentials credentials = authRepository.getCredentials(jid, Credentials.DEFAULT_CREDENTIAL_ID);
		if (credentials == null) {
			return false;
		}

		return Optional.ofNullable(credentials.getFirst()).map(e -> e.verifyPlainPassword(password)).orElse(false);
	}
}