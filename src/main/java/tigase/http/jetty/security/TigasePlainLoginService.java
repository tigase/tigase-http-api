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

import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Password;
import tigase.http.AuthProvider;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
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
	public UserIdentity login(String s, Object o, ServletRequest request) {
		if (request instanceof HttpServletRequest) {
			AuthProvider.JWTPayload payload = authProvider.authenticateWithCookie((HttpServletRequest) request);
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
		Principal p = new AbstractLoginService.UserPrincipal(jid.toString(), null);

		Subject subject = new Subject();
		subject.getPrincipals().add(p);
		subject.setReadOnly();

		List<String> roles = getAuthProvider().getRoles(jid);

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
