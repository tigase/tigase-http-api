/*
 * TigasePlainLoginService.groovy
 *
 * Tigase HTTP API - Jetty support
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
package tigase.http.jetty.security

import org.eclipse.jetty.security.DefaultIdentityService
import org.eclipse.jetty.security.IdentityService
import org.eclipse.jetty.security.LoginService
import org.eclipse.jetty.security.MappedLoginService
import org.eclipse.jetty.server.UserIdentity
import org.eclipse.jetty.util.security.Password
import tigase.http.api.Service
import tigase.xmpp.jid.BareJID

import javax.security.auth.Subject
import javax.servlet.ServletRequest
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger

public class TigasePlainLoginService
		implements LoginService {

	private static final Logger log = Logger.getLogger(TigasePlainLoginService.class.getCanonicalName());

	def identityService = new DefaultIdentityService();

	Service service = null;

	public TigasePlainLoginService(Service service) {
		this.service = service;
	}

	@Override
	String getName() {
		return "TigasePlain";
	}

	@Override
	UserIdentity login(String s, Object o, ServletRequest request) {
		String cred = null;
		if (o instanceof String) {
			cred = (String) o
		};
		if (o instanceof Password) {
			cred = ((Password) o).toString()
		};

		if (cred != null) {
			// decode credential
			BareJID jid = BareJID.bareJIDInstance(s);

			// authenticate using Tigase authentication repository
			boolean authOk = false;
			try {
				authOk = getService().checkCredentials(s, cred);
			} catch (ex) {
				log.log(Level.FINE, "not authorized used = " + jid, ex);
			}

			// we are authenticated so set correct authentication principal
			if (authOk) {
				Principal p = new MappedLoginService.KnownUser(s, null);

				Subject subject = new Subject();
				subject.getPrincipals().add(p);
				subject.setReadOnly();

				def roles = [ "user" ];

				// add admin role if user is in admins list
				if (getService().isAdmin(jid)) {
					roles.add("admin")
				};

				return getIdentityService().newUserIdentity(subject, p, roles.toArray(new String[0]));
			}
		}

		return null  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	boolean validate(UserIdentity userIdentity) {
		// validate if user identity is valid
		return getService().
				getUserRepository().
				getUserUID(BareJID.bareJIDInstance(userIdentity.getUserPrincipal().getName())) > 0;
	}

	@Override
	IdentityService getIdentityService() {
		return identityService;
	}

	@Override
	void setIdentityService(IdentityService identityService2) {
		identityService = identityService2;
	}

	@Override
	void logout(UserIdentity userIdentity) {
	}

	Service getService() {
		return service;
	}

}
