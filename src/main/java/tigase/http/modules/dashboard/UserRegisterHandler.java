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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.http.jaxrs.Model;
import tigase.http.jaxrs.annotations.JidLocalpart;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.server.xmppsession.SessionManager;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostManager;
import tigase.xmpp.XMPPProcessorException;
import tigase.xmpp.impl.CaptchaProvider;
import tigase.xmpp.impl.JabberIqRegister;
import tigase.xmpp.jid.BareJID;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.lang.reflect.Field;
import java.util.stream.Collectors;

@Bean(name = "register", parent = DashboardModule.class, active = true)
@Path("/users/register")
public class UserRegisterHandler extends DashboardHandler {

	@Inject
	private AuthRepository authRepository;
	@Inject
	private UserRepository userRepository;
	@Inject
	private VHostManager vHostManager;
	@Inject
	private SessionManager sessionManager;

	@Override
	public Role getRequiredRole() {
		return Role.None;
	}

	@GET
	@Path("")
	@Produces(MediaType.TEXT_HTML)
	public Response form(UriInfo uriInfo, Model model) {
		JabberIqRegister register = getJabberIqRegister();
		if (register == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		var domains = vHostManager.getAllVHosts(false)
				.stream()
				.map(domain -> vHostManager.getVHostItem(domain.getDomain()))
				.filter(vhost -> vhost.isEnabled() && vhost.isRegisterEnabled())
				.map(vhost -> vhost.getVhost().getDomain())
				.sorted()
				.collect(Collectors.toList());
		int indexOfDomain = domains.indexOf(uriInfo.getBaseUri().getHost());
		if (indexOfDomain != -1) {
			String domain = domains.remove(indexOfDomain);
			domains.addFirst(domain);
		}


		model.put("domains", domains);
		model.put("emailRequired", register.isEmailRequired());
		if (register.isCaptchaRequired()) {
			CaptchaProvider.SimpleTextCaptcha captcha = generateCaptcha();
			model.put("captcha", captcha.getCaptchaRequest());
			model.put("captchaID", captcha.getID());
		}
		String output = renderTemplate("users/register.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	@POST
	@Path("")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_HTML)
	public Response register(@FormParam("domain") @NotEmpty String domain,
							 @FormParam("username") @JidLocalpart(message = "is not a valid username") String username,
							 @FormParam("password") @NotEmpty String password, @FormParam("email") @Email String email,
							 @FormParam("captcha") String captchaResponse, @FormParam("captchaID") String captchaID,
							 Model model) throws TigaseStringprepException, TigaseDBException {
		JabberIqRegister register = getJabberIqRegister();
		if (register.isEmailRequired()) {
			if (email == null || email.isEmpty()) {
				return Response.status(Response.Status.BAD_REQUEST).build();
			}
		}
		if (register.isCaptchaRequired()) {
			if (!validateCaptcha(captchaID, captchaResponse)) {
				return Response.status(Response.Status.BAD_REQUEST).build();
			}
		}

		VHostItem vhost = vHostManager.getVHostItem(domain);
		if (!vhost.isRegisterEnabled()) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		if (vhost.getMaxUsersNumber() > 0) {
			long domainUsers = authRepository.getUsersCount(domain);

			if (domainUsers >= vhost.getMaxUsersNumber()) {
				return Response.status(Response.Status.NOT_ACCEPTABLE).build();
			}
		}

		BareJID user = BareJID.bareJIDInstance(username, domain);
		try {
			register.createAccount(authRepository, user, password, email, null);
		} catch (XMPPProcessorException ex) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		model.put("jid", user);
		model.put("confirmationRequired", authRepository.getAccountStatus( user) == AuthRepository.AccountStatus.pending);

		String output = renderTemplate("users/registered.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	public boolean isRegistrationEnabled() {
		return getJabberIqRegister() != null;
	}
	
	private JabberIqRegister getJabberIqRegister() {
		try {
			Field f = SessionManager.class.getDeclaredField("kernel");
			f.setAccessible(true);
			Kernel kernel = (Kernel) f.get(sessionManager);
			if (kernel == null) {
				return null;
			}
			return kernel.getInstanceIfExistsOr(JabberIqRegister.ID, x -> null);
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

}
