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
package tigase.http.modules.rest.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.http.api.HttpException;
import tigase.http.api.NotFoundException;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.xmpp.jid.BareJID;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

@Bean(name = "user", parent = RestModule.class, active = true)
@Path("/user")
public class UserHandler extends AbstractRestHandler {

	@Inject
	private AuthRepository authRepository;
	@Inject
	private UserRepository userRepository;

	public UserHandler() {
		super(Security.ApiKey, Role.None);
	}

	@GET
	@Path("/{userJid}")
	@Produces({"application/json", "application/xml"})
	@Operation(summary = "Get user", description = "Get user details")
	@ApiResponse(responseCode = "200", description = "User details", content = { @Content(schema = @Schema(implementation = User.class)) })
	@ApiResponse(responseCode = "404", description = "User not found")
	public User getUser(@Parameter(description = "Bare JID of the user") @NotNull @PathParam("userJid") BareJID userJid)
			throws HttpException {
		if (!userRepository.userExists(userJid)) {
			throw new NotFoundException("User does not exist!");
		}

		return new User(userJid, userJid.getDomain(), null);
	}

	@PUT
	@Path("/{userJid}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	@Operation(summary = "Create user", description = "Create new user")
	@ApiResponse(responseCode = "409", description = "User already exists")
	public User createUser(
			@Parameter(description = "Bare JID of the user") @NotNull @PathParam("userJid") BareJID userJid,
			@NotNull UserCreate form) throws HttpException, TigaseDBException {
		if (userRepository.userExists(userJid)) {
			throw new HttpException("User already exists!", HttpServletResponse.SC_CONFLICT);
		}

		authRepository.addUser(userJid, form.password);
		userRepository.setData(userJid, "email", form.email);

		return new User(userJid, userJid.getDomain(), Result.created);
	}

	@POST
	@Path("/{userJid}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	@Operation(summary = "Modify user", description = "Modify user details")
	@ApiResponse(responseCode = "404", description = "User not found")
	public User changeUser(
			@Parameter(description = "Bare JID of the user") @NotNull @PathParam("userJid") BareJID userJid,
			@NotNull UserChange form)
			throws NotFoundException, TigaseDBException {
		if (!userRepository.userExists(userJid)) {
			throw new NotFoundException("User does not exist!");
		}

		authRepository.updateCredential(userJid, "default", form.password);
		if (form.email != null && !form.email.isEmpty()) {
			userRepository.setData(userJid, "email", form.email);
		}

		return new User(userJid, userJid.getDomain(), Result.updated);
	}

	@DELETE
	@Path("/{userJid}")
	@Produces({"application/json", "application/xml"})
	@Operation(summary = "Remove user", description = "Remove user")
	@ApiResponse(responseCode = "404", description = "User not found")
	public User removeUser(
			@Parameter(description = "Bare JID of the user") @NotNull @PathParam("userJid") BareJID userJid)
			throws NotFoundException, TigaseDBException {
		if (!userRepository.userExists(userJid)) {
			throw new NotFoundException("User does not exist!");
		}

		authRepository.removeUser(userJid);
		try {
			userRepository.removeUser(userJid);
		} catch (UserNotFoundException ex) {
			// We ignore this error here. If auth_repo and user_repo are in fact the same
			// database, then user has been already removed with the auth_repo.removeUser(...)
			// then the second call to user_repo may throw the exception which is fine.
		}

		return new User(userJid, userJid.getDomain(), Result.deleted);
	}

	public enum Result {
		created,
		updated,
		deleted
	}

	@XmlRootElement(name = "user")
	public static class User {
		@XmlAttribute
		@NotNull
		private BareJID jid;
		@XmlAttribute
		@NotNull
		private String domain;
		@XmlAttribute
		private Result result;

		public User(){}

		public User(BareJID userJid, String domain, Result result) {
			this.jid = userJid;
			this.domain = domain;
			this.result = result;
		}

		public BareJID getJid() {
			return jid;
		}

		public void setJid(BareJID jid) {
			this.jid = jid;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public Result getResult() {
			return result;
		}

		public void setResult(Result result) {
			this.result = result;
		}
	}

	@XmlRootElement(name = "user")
	public static class UserCreate {
		@XmlElement
		@NotNull
		private String password;
		@XmlElement
		@NotNull
		private String email;

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}

	@XmlRootElement(name = "user")
	public static class UserChange {
		@XmlElement
		@NotNull
		private String password;
		@XmlElement
		private String email;

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}

}
