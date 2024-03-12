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
package tigase.http.jaxrs;

import jakarta.ws.rs.core.SecurityContext;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

public class SecurityContextImpl implements SecurityContext {

	private HttpServletRequest request;

	public SecurityContextImpl(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public Principal getUserPrincipal() {
		return request.getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String s) {
		return request.isUserInRole(s);
	}

	@Override
	public boolean isSecure() {
		return "https".equalsIgnoreCase(request.getProtocol());
	}

	@Override
	public String getAuthenticationScheme() {
		return request.getAuthType();
	}
}