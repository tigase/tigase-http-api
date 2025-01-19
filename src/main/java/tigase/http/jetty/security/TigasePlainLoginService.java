/*
 * Tigase HTTP API - Jetty - Tigase HTTP API - support for Jetty HTTP Server
 * Copyright (C) 2014 Tigase, Inc. (office@tigase.com)
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
package tigase.http.jetty.security;

import org.eclipse.jetty.ee10.servlet.ServletContextRequest;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.security.Password;
import tigase.http.AuthProvider;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TigasePlainLoginService
		implements LoginService {

	private static final Logger log = Logger.getLogger(TigasePlainLoginService.class.getCanonicalName());
	private DefaultIdentityService identityService = new DefaultIdentityService();
	private AuthProvider authProvider = null;

	public TigasePlainLoginService(AuthProvider authProvider) {
		this.authProvider = authProvider;
	}

	@Override
	public String getName() {
		return "TigasePlain";
	}

	@Override
	public UserIdentity login(String s, Object o, Request request, Function<Boolean, Session> getOrCreateSession) {
		if (request instanceof ServletContextRequest) {
			AuthProvider.JWTPayload payload = authProvider.authenticateWithCookie(((ServletContextRequest) request).getServletApiRequest());
			if (payload != null) {
				return newUserIdentity(payload.subject());
			}
		}

		String cred = null;
		if (o instanceof String) {
			cred = (String) o;
		}

		if (o instanceof Password) {
			cred = ((Password) o).toString();
		}

		if (cred != null) {
			// decode credential
			BareJID jid = BareJID.bareJIDInstanceNS(s);

			// authenticate using Tigase authentication repository
			boolean authOk = false;
			try {
				authOk = getAuthProvider().checkCredentials(s, cred);
			} catch (Exception ex) {
				log.log(Level.FINE, "not authorized used = " + jid, ex);
			}

			// we are authenticated so set correct authentication principal
			if (authOk) {
				return newUserIdentity(jid);
			}

		}
		return null;
	}

	private UserIdentity newUserIdentity(BareJID jid) {
		Principal p = new UserPrincipal(jid.toString(), null);

		Subject subject = new Subject();
		subject.getPrincipals().add(p);
		subject.setReadOnly();

		List<String> roles = new ArrayList<String>(Arrays.asList("user"));

		// add admin role if user is in admins list
		if (getAuthProvider().isAdmin(jid)) {
			roles.add("admin");
		}

		return getIdentityService().newUserIdentity(subject, p, roles.toArray(new String[0]));
	}

	@Override
	public boolean validate(UserIdentity userIdentity) {
		// validate if user identity is valid
		return true;
	}

	@Override
	public IdentityService getIdentityService() {
		return ((IdentityService) (identityService));
	}

	@Override
	public void setIdentityService(IdentityService identityService2) {
		identityService = ((DefaultIdentityService) (identityService2));
	}

	@Override
	public void logout(UserIdentity userIdentity) {
	}

	public AuthProvider getAuthProvider() {
		return authProvider;
	}

	public void setAuthProvider(AuthProvider authProvider) {
		this.authProvider = authProvider;
	}

}
