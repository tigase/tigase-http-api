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
package tigase.http.modules.rest;

import tigase.kernel.beans.config.ConfigField;

public abstract class AbstractRestHandler implements RestHandler {

	@ConfigField(desc = "Required role of authorized user")
	private Role requiredRole;

	@ConfigField(desc = "Security")
	private Security security;

	protected AbstractRestHandler() {
		this(Security.ApiKey, Role.None);
	}

	protected AbstractRestHandler(Security security, Role requiredRole) {
		this.security = security;
		this.requiredRole = requiredRole;
	}

	@Override
	public Role getRequiredRole() {
		return requiredRole;
	}

	@Override
	public Security getSecurity() {
		return security;
	}
}
