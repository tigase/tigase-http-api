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

import jakarta.ws.rs.core.SecurityContext;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.http.jaxrs.SecurityContextHolder;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostManager;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Bean(name = "permissionsHelper", parent = DashboardModule.class, active = true)
public class PermissionsHelper {

	@Inject
	private DashboardModule dashboardModule;
	@Inject
	private UserRepository userRepository;
	@Inject
	private VHostManager vHostManager;

	@ConfigField(desc = "User can create domain")
	private boolean userCanCreateDomain = false;

	public boolean canCreateDomain() {
		SecurityContext securityContext = SecurityContextHolder.getSecurityContext();
		if (securityContext == null) {
			return false;
		}
		if (securityContext.isUserInRole("admin")) {
			return true;
		}
		return securityContext.isUserInRole("user") && userCanCreateDomain;
	}

	public List<String> getManagedDomains(SecurityContext securityContext) {
		Stream<JID> domains = vHostManager.getAllVHosts()
				.stream();
		if (!(securityContext.isUserInRole("admin") || securityContext.isUserInRole("account_manager"))) {
			if (securityContext.isUserInRole("user")) {
				domains = domains.filter(domain -> canManageDomain(securityContext, domain.getDomain()));
			} else {
				return Collections.emptyList();
			}
		}
		return domains
				.map(JID::getDomain)
				.filter(domain -> !"default".equals(domain))
				.sorted()
				.toList();
	}

	public boolean canManageDomain(SecurityContext securityContext, String domain) {
		if (securityContext.isUserInRole("admin") || securityContext.isUserInRole("account_manager")) {
			return true;
		} else {
			VHostItem item = vHostManager.getVHostItem(domain);
			String user = securityContext.getUserPrincipal().getName();
			return item != null && (item.isAdmin(user) || item.isOwner(user));
		}
	}

	public List<String> getUserRolesIds(BareJID user) throws TigaseDBException {
		var roles = new ArrayList<String>();
		if (dashboardModule.isAdmin(user)) {
			roles.add("admin");
		}
		String[] rolesFromRepo = userRepository.getDataList(user, "roles", "roles");
		if (rolesFromRepo != null) {
			roles.addAll(Arrays.asList(rolesFromRepo));
		}
		return roles;
	}

	public boolean canManageUser(BareJID jid) {
		try {
			var securityContext = SecurityContextHolder.getSecurityContext();
			if (securityContext != null) {
				if (securityContext.isUserInRole("admin")) {
					return true;
				}
				if (securityContext.isUserInRole("account_manager")) {
					var managedUserRoles = getUserRolesIds(jid);
					return (!(managedUserRoles.contains("admin") || managedUserRoles.contains("account_manager"))) || securityContext.getUserPrincipal().getName().equals(jid.toString());
				}
				if (securityContext.isUserInRole("user")) {
					return canManageDomain(securityContext, jid.getDomain()) || securityContext.getUserPrincipal().getName().equals(jid.toString());
				}
			}
		} catch (TigaseDBException e) {
		}
		return false;
	}
}
