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
package tigase.http.modules.dashboard;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import tigase.auth.credentials.entries.XTokenCredentialsEntry;
import tigase.auth.mechanisms.SaslXTOKEN;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.http.jaxrs.Model;
import tigase.http.jaxrs.Page;
import tigase.http.jaxrs.Pageable;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.xmppsession.DisconnectUserEBAction;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostManager;
import tigase.xmpp.StreamError;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@Bean(name = "users", parent = DashboardModule.class, active = true)
@Path("/users")
public class UsersHandler extends DashboardHandler {

	private final SecureRandom secureRandom = new SecureRandom();
	@Inject
	private AuthRepository authRepository;
	@Inject
	private UserRepository userRepository;
	@Inject
	private VHostManager vHostManager;
	@Inject
	private EventBus eventBus;

	public UsersHandler() {
		super();
	}

	@Override
	public Role getRequiredRole() {
		return Role.Admin;
	}

	@GET
	@Path("")
	@Produces("text/html")
	public Response index(@QueryParam("query") String query, Pageable pageable, Model model) throws TigaseDBException {
		List<String> domains = vHostManager.getAllVHosts()
				.stream()
				.map(JID::getDomain)
				.filter(domain -> !"default".equals(domain))
				.sorted()
				.toList();
		Set<String> domainsSet = new HashSet<>(domains);
		List<BareJID> jids = userRepository.getUsers()
				.stream()
				.filter(jid -> jid.getLocalpart() != null)
				.filter(jid -> domainsSet.contains(jid.getDomain()))
				.filter(jid -> query == null || jid.toString().contains(query))
				.sorted(Comparator.comparing(BareJID::getLocalpart).thenComparing(BareJID::getDomain))
				.toList();
		List<User> users = jids.stream()
				.skip(pageable.offset())
				.limit(pageable.pageSize())
				.map(jid -> {
					try {
						return new User(jid, authRepository.getAccountStatus(jid));
					} catch (TigaseDBException e) {
						throw new RuntimeException(e);
					}
				})
				.toList();
		model.put("query", query);
		model.put("users", new Page<>(pageable, jids.size(), users));
		model.put("domains", domains);

		model.put("isXTokenActive", authRepository.isMechanismSupported("default", SaslXTOKEN.NAME));

		String output = renderTemplate("users/index.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createUser(@FormParam("localpart") @NotEmpty String localpart,
							   @FormParam("domain") @NotEmpty String domain, @FormParam("password") String password,
							   UriInfo uriInfo) throws TigaseStringprepException, TigaseDBException {
		if (localpart.isBlank() || domain.isBlank()) {
			throw new RuntimeException();
		}
		BareJID jid = BareJID.bareJIDInstance(localpart, domain);
		if (userRepository.userExists(jid)) {
			throw new RuntimeException("User already exist!");
		}
		if (password != null && !password.trim().isBlank()) {
			authRepository.addUser(jid, password);
			authRepository.setAccountStatus(jid, AuthRepository.AccountStatus.active);
		} else {
			userRepository.addUser(jid);
		}
		return redirectToIndex(uriInfo, jid.toString());
	}

	@POST
	@Path("/{jid}/delete")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response deleteUser(@PathParam("jid") @NotEmpty BareJID jid, UriInfo uriInfo) throws TigaseDBException {
		authRepository.removeUser(jid);
		eventBus.fire(new DisconnectUserEBAction(jid, StreamError.Reset,
												 "Account was deleted"));
		return redirectToIndex(uriInfo);
	}

	@GET
	@Path("/{jid}/accountStatus/{accountStatus}")
	public Response changeAccountStatus(@PathParam("jid") @NotEmpty BareJID jid, @PathParam("accountStatus")
										AuthRepository.AccountStatus accountStatus, UriInfo uriInfo)
			throws TigaseDBException {
		authRepository.setAccountStatus(jid, accountStatus);
		switch (accountStatus) {
			case disabled, spam, banned -> logoutUser(jid);
			default -> {}
		}
		return redirectToIndex(uriInfo);
	}

	@POST
	@Path("/{jid}/password")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response changePassword(@PathParam("jid") @NotEmpty BareJID jid, @FormParam("password") @NotBlank String password,
								   @FormParam("password-confirm") @NotBlank String passwordConfirm, UriInfo uriInfo)
			throws TigaseDBException {
		if (!password.equals(passwordConfirm)) {
			throw new RuntimeException("Passwords do not match!");
		}
		authRepository.updateCredential(jid, "default", password);
		authRepository.setAccountStatus(jid, AuthRepository.AccountStatus.active);

		logoutUser(jid);

		return redirectToIndex(uriInfo);
	}

	public static Response redirectToIndex(UriInfo uriInfo) {
		return redirectToIndex(uriInfo, null);
	}
	
	public static Response redirectToIndex(UriInfo uriInfo, String query) {
		return Response.seeOther(uriInfo.getBaseUriBuilder().path(UsersHandler.class, "index").replaceQueryParam("query", query).build()).build();
	}

	@POST
	@Path("/{jid}/qrCode")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("image/png")
	public Response generateAuthQrCodePng(@PathParam("jid") @NotEmpty BareJID jid)
			throws IOException, WriterException, TigaseDBException {
		String token = generateAuthQrCodeToken(jid);
		return Response.ok(encodeStringToQRCode(token), "image/png").build();
	}

	@POST
	@Path("/{jid}/qrCode")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public QRCode generateAuthQrCodeJson(@PathParam("jid") @NotEmpty BareJID jid)
			throws IOException, WriterException, TigaseDBException {
		String token = generateAuthQrCodeToken(jid);
		byte[] qrcode = encodeStringToQRCode(token);
		return new QRCode(token, "data:image/png;base64," + Base64.encode(qrcode));
	}

	public static class QRCode {
		private final String token;
		private final String png;

		public QRCode(String token, String png) {
			this.token = token;
			this.png = png;
		}

		public String getToken() {
			return token;
		}

		public String getPng() {
			return png;
		}
	}

	private byte[] encodeStringToQRCode(String token) throws IOException, WriterException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(token.toString(), BarcodeFormat.QR_CODE, 300, 300, Map.of(
				EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8, EncodeHintType.MARGIN, 0));
		MatrixToImageConfig imageConfig = new MatrixToImageConfig(MatrixToImageConfig.BLACK, MatrixToImageConfig.WHITE);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos, imageConfig);
		return baos.toByteArray();
	}


	private String generateAuthQrCodeToken(BareJID jid) throws TigaseDBException {
		byte[] secret = new byte[32];
		secureRandom.nextBytes(secret);

		byte[] jidBytes = jid.toString().getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[secret.length + 1 + jidBytes.length];
		System.arraycopy(secret, 0, data, 0, secret.length);
		System.arraycopy(jidBytes, 0, data, secret.length + 1, jidBytes.length);
		String token = Base64.encode(data);

		authRepository.removeCredential(jid, "default");
		authRepository.updateCredential(jid, "default", SaslXTOKEN.NAME, new XTokenCredentialsEntry(secret, true).encoded());
		authRepository.setAccountStatus(jid, AuthRepository.AccountStatus.active);

		logoutUser(jid);

		return token;
	}

	private void logoutUser(BareJID jid) throws TigaseDBException {
		eventBus.fire(new DisconnectUserEBAction(jid, StreamError.Reset,
		                                         "Account credentials were changed, please login again with new credentials"));
		authRepository.logout(jid);
	}

	public record User(BareJID jid, AuthRepository.AccountStatus accountStatus) {}
}
