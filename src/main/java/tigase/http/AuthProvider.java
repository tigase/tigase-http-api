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

import tigase.db.TigaseDBException;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

public interface AuthProvider {

	boolean isAdmin(BareJID user);

	boolean checkCredentials(String user, String password)
			throws TigaseStringprepException, TigaseDBException;

	String generateToken(JWTPayload token)
			throws NoSuchAlgorithmException, InvalidKeyException;

	JWTPayload parseToken(String token) throws AuthenticationException;

	record JWTPayload(BareJID subject, String issuer, LocalDateTime expireAt) {
	}

	JWTPayload authenticateWithCookie(HttpServletRequest request);

	void setAuthenticationCookie(HttpServletResponse response, JWTPayload payload, String domain, String path)
			throws NoSuchAlgorithmException, InvalidKeyException;

	void resetAuthenticationCookie(HttpServletResponse response, String domain, String path);

	void refreshJwtToken(HttpServletRequest request, HttpServletResponse response);
}