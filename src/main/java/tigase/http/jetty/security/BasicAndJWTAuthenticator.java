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

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class BasicAndJWTAuthenticator extends BasicAuthenticator
		implements Authenticator {

	@Override
	public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory)
			throws ServerAuthException {
		if (mandatory) {
			UserIdentity userIdentity = getLoginService().login(null, null, req);
			if (userIdentity != null) {
			return new UserAuthentication("TOKEN", userIdentity);
			}
		}
		return super.validateRequest(req, res, mandatory);
	}
}
