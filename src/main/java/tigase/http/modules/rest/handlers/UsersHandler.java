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
package tigase.http.modules.rest.handlers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.xmpp.jid.BareJID;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Bean(name = "users", parent = RestModule.class, active = true)
@Path("/users")
public class UsersHandler extends AbstractRestHandler {

	@Inject
	private UserRepository userRepository;

	public UsersHandler() {
		super(Security.ApiKey, Role.None);
	}

	@GET
	@Path("/")
	@Produces({"application/json","application/xml"})
	@Operation(summary = "Get list of user", description = "Get list of user")
	@ApiResponse(responseCode = "200", description = "List of users", content = {@Content(schema = @Schema(implementation = Users.class)) })
	public Users listUsers() throws TigaseDBException {
		List<BareJID> users = userRepository.getUsers();
		return new Users(users, users.size());
	}

	@GET
	@Path("/{domain}")
	@Produces({"application/json","application/xml"})
	@Operation(summary = "Get list of user for domain", description = "Get list of user for domain")
	@ApiResponse(responseCode = "200", description = "List of users", content = {@Content(schema = @Schema(implementation = Users.class)) })
	public Users listUsersFromDomain(@Parameter(description = "Domain to list users for") @NotNull @PathParam("domain") String domain) throws TigaseDBException {
		List<BareJID> users = userRepository.getUsers()
				.stream()
				.filter(jid -> domain.equals(jid.getDomain()))
				.collect(Collectors.toList());
		return new Users(users, users.size());
	}

	@GET
	@Path("/async")
	@Produces({"application/json","application/xml"})
	@Operation(summary = "DEMO: Get list of user for domain (async)", description = "Get list of user for domain")
	@ApiResponse(responseCode = "200", description = "List of users", content = {@Content(schema = @Schema(implementation = Users.class)) })
	public void listUsersAsync(@Suspended final AsyncResponse response) {
		List<BareJID> jids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			jids.add(BareJID.bareJIDInstanceNS("user-" + i, "example.com"));
		}
		new Thread(() -> {
			try {
				Thread.sleep(2000);
				response.resume(new Users(jids, jids.size()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	@XmlRootElement(name = "users")
	public static class Users {

		@XmlElement(name = "user")
		private List<BareJID> users;
		private int count;

		public Users() {}

		public Users(List<BareJID> users, int count) {
			this.users = users;
			this.count = count;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public List<BareJID> getUsers() {
			return users;
		}

		public void setUsers(List<BareJID> users) {
			this.users = users;
		}
	}

}
