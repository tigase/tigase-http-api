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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tigase.auth.credentials.Credentials;
import tigase.db.*;
import tigase.http.json.JsonParser;
import tigase.http.json.JsonSerializer;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.AuthenticationException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Bean(name = "authProvider", parent = HttpMessageReceiver.class, active = true, exportable = true)
public class AuthProviderImpl
		implements AuthProvider, Initializable {

	private static final String JWT_SECRET_KEY = "jwtSecretKey";
	@Inject(nullAllowed = true)
	private UserRepository userRepository;
	@Inject(nullAllowed = true)
	private AuthRepository authRepository;
	@Inject(bean = "service")
	private HttpMessageReceiver receiver;

	private SecretKeySpec secretKey;
	private final JsonSerializer jsonSerializer = new JsonSerializer();

	public AuthProviderImpl() {
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
	public String generateToken(JWTPayload token)
			throws NoSuchAlgorithmException, InvalidKeyException {
		Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
		Map<String, Object> payload = Map.of("sub", token.subject().toString(), "iss", token.issuer(), "exp",
			   token.expireAt().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());

		String data = Base64.encode(jsonSerializer.serialize(header).getBytes(StandardCharsets.UTF_8)) + "." +
				Base64.encode(jsonSerializer.serialize(payload).getBytes(StandardCharsets.UTF_8));
		return data + "." + calculateTokenSignature(data);
	}

	private String calculateTokenSignature(String data) throws InvalidKeyException, NoSuchAlgorithmException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(secretKey);
		return Base64.encode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
	}

	@Override
	public JWTPayload parseToken(String token) throws AuthenticationException {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new AuthenticationException("Incorrect JWT token");
		}

		try {
			Map<String, Object> header = (Map<String, Object>) JsonParser.parseJson(Base64.decode(parts[0]));
			if ("HS256".equals(header.get("alg")) && "JWT".equals(header.get("typ"))) {
				if (parts[2].equals(calculateTokenSignature(parts[0] + "." + parts[1]))) {
					Map<String, Object> payload = (Map<String, Object>) JsonParser.parseJson(Base64.decode(parts[1]));
					Number exp = (Number) payload.get("exp");
					LocalDateTime expireAt = Instant.ofEpochMilli(exp.longValue())
							.atZone(ZoneId.of("UTC"))
							.toLocalDateTime();
					if (expireAt.isBefore(LocalDateTime.now())) {
						throw new AuthenticationException("JWT token is expired");
					}
					BareJID subject = BareJID.bareJIDInstance((String) payload.get("sub"));
				 	String issuer = (String) payload.get("iss");
					return new JWTPayload(subject, issuer, expireAt);
				}
			}
			throw new AuthenticationException("Incorrect JWT token");
		} catch (AuthenticationException ex) {
			throw ex;
		} catch (Throwable ex) {
			throw new AuthenticationException("Incorrect JWT token");
		}
	}

	public void refreshJwtToken(HttpServletRequest request, HttpServletResponse response) {
		JWTPayload payload = (JWTPayload) request.getAttribute("refreshJwtToken");
		if (payload != null) {
			try {
				System.out.println("refreshing JWT token for " + payload.subject());
				setAuthenticationCookie(response, new JWTPayload(payload.subject(), payload.issuer(),
																 LocalDateTime.now().plusMinutes(15)),
										request.getServerName(), request.getContextPath());
			} catch (Throwable ignored) {
			}
		}
	}

	public JWTPayload authenticateWithCookie(HttpServletRequest request) {
		Cookie[] cookies =  request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (!cookie.getName().equals("jwtToken")) {
					continue;
				}
				try {
					JWTPayload payload = parseToken(cookie.getValue());
					if (payload != null && payload.expireAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
						request.setAttribute("refreshJwtToken", payload);
					}
					return payload;
				} catch (AuthenticationException e) {
				}
			}
		}
		return null;
	}

	public void setAuthenticationCookie(HttpServletResponse response, JWTPayload payload, String domain, String path)
			throws NoSuchAlgorithmException, InvalidKeyException {
		String value = generateToken(payload);
		setAuthCookie(response, domain, path, value, payload.expireAt());
	}

	public void resetAuthenticationCookie(HttpServletResponse response, String domain, String path) {
		setAuthCookie(response, domain, path , "", LocalDateTime.now().minusDays(1));
	}

	private void setAuthCookie(HttpServletResponse response, String domain, String path, String value, LocalDateTime expireAt) {
		StringBuilder sb = new StringBuilder("jwtToken").append("=").append(value);
		sb.append("; ").append("Domain=").append(domain).append("; Path=").append(path);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");
		String expireAtStr = expireAt.atZone(ZoneId.of("GMT")).format(formatter);
		String cookieValue = sb.append("; ").append("Expires=").append(expireAtStr).toString();
		response.setHeader("Set-Cookie", cookieValue);
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