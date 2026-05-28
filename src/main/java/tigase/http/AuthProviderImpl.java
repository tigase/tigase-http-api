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
import tigase.db.*;
import tigase.http.json.JsonSerializer;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Bean(name = "authProvider", parent = HttpMessageReceiver.class, active = true, exportable = true)
public class AuthProviderImpl
		extends AbstractAuthProvider
		implements AuthProvider, Initializable {

	private static final String JWT_SECRET_KEY = "jwtSecretKey";
	@Inject(nullAllowed = true)
	private UserRepository userRepository;
	@Inject(nullAllowed = true)
	private AuthRepository authRepository;
	@Inject(bean = "service")
	private HttpMessageReceiver receiver;
	@ConfigField(desc = "Authentication token validity time", alias = "auth-expiration")
	private Duration authenticationTokenValidityDuration = Duration.ofMinutes(30);

	private SecretKeySpec secretKey;
	private final JsonSerializer jsonSerializer = new JsonSerializer();

	public AuthProviderImpl() {
	}

	public Duration getAuthenticationTokenValidityDuration() {
		return authenticationTokenValidityDuration;
	}

	protected SecretKeySpec getSecretKey() {
		return secretKey;
	}

	@Override
	public void initialize() {
		if (userRepository != null) {
			BareJID user = BareJID.bareJIDInstanceNS(receiver.getName());

			try {
				try {
					if (!userRepository.userExists(user)) {
						userRepository.addUser(user);
					}
				} catch (UserExistsException e) {
				}
				
				String secretKeyStr = userRepository.getData(user, JWT_SECRET_KEY);
				if (secretKeyStr == null) {
					SecureRandom random = new SecureRandom();
					byte[] secret = new byte[32];
					random.nextBytes(secret);
					String newSecretKeyStr = Base64.encode(secret);
					secretKeyStr = userRepository.getData(user, JWT_SECRET_KEY);
					if (secretKeyStr == null) {
						userRepository.setData(user, JWT_SECRET_KEY, newSecretKeyStr);
						Thread.sleep(500);
						secretKeyStr = userRepository.getData(user, JWT_SECRET_KEY);
					}

//					Mac mac = Mac.getInstance("HmacSHA256");
//					mac.init(secretKey);
//					mac.doFinal("".getBytes(StandardCharsets.UTF_8));
				}
				secretKey = new SecretKeySpec(Base64.decode(secretKeyStr), "HmacSHA256");
			} catch (Throwable ex) {
				throw new RuntimeException("Failed to generate and store secret key!", ex);
			}
		}
	}

	@Override
	public boolean isAdmin(BareJID user) {
		return receiver.isAdmin(JID.jidInstance(user));
	}

	@Override
	public List<String> getRoles(BareJID user) {
		var roles = super.getRoles(user);
		try {
			String[] rolesFromRepo = userRepository.getDataList(user, "roles", "roles");
			if (rolesFromRepo != null) {
				roles.addAll(Arrays.asList(rolesFromRepo));
			}
		} catch (TigaseDBException ex) {
			throw new RuntimeException("Failed to load user " + user + " roles", ex);
		}
		return roles;
	}
	
	@Override
	public boolean checkCredentials(String user, final String password)
			throws TigaseStringprepException, TigaseDBException {
		if (authRepository == null) {
			return false;
		}

		try {
			BareJID jid = BareJID.bareJIDInstance(user);
			Credentials credentials = authRepository.getCredentials(jid, Credentials.DEFAULT_CREDENTIAL_ID);
			if (credentials == null) {
				return false;
			}

			return Optional.ofNullable(credentials.getFirst()).map(e -> e.verifyPlainPassword(password)).orElse(false);
		} catch (UserNotFoundException ex) {
			return false;
		}
	}
}