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

public class SecurityContextHolder {
	
	public static void setSecurityContext(SecurityContext securityContext) {
		ContainerRequestContext context = ContainerRequestContext.getContext();
		if (context != null) {
			context.setSecurityContext(securityContext);
		}
	}

	public static SecurityContext getSecurityContext() {
		ContainerRequestContext context = ContainerRequestContext.getContext();
		if (context != null) {
			return context.getSecurityContext();
		}
		return null;
	}

	public static void resetSecurityContext() {
		ContainerRequestContext context = ContainerRequestContext.getContext();
		if (context != null) {
			context.setSecurityContext(null);
		}
	}

}
