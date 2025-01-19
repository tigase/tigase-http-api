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

import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class BasicAndJWTAuthenticator extends BasicAuthenticator
		implements Authenticator {

	@Override
	public AuthenticationState validateRequest(Request req, Response res, Callback callback)
			throws ServerAuthException {
		UserIdentity userIdentity = getLoginService().login(null, null, req, x -> null);
		if (userIdentity != null) {
			return new UserAuthenticationSucceeded("TOKEN", userIdentity);
		}
		return super.validateRequest(req, res, callback);
	}
}
