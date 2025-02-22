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

import jakarta.annotation.security.RolesAllowed;

import java.lang.reflect.Method;
import java.util.Set;

public interface Handler {

	public static Set<String> getAllowedRoles(Method method) {
		var allowedRoles = getAllowedRoles(method.getAnnotation(RolesAllowed.class));
		if (allowedRoles != null) {
			return allowedRoles;
		}
		return getAllowedRoles(method.getDeclaringClass().getAnnotation(RolesAllowed.class));
	}

	public static Set<String> getAllowedRoles(RolesAllowed annotation) {
		if (annotation == null) {
			return null;
		}
		return Set.of(annotation.value());
	}

	Role getRequiredRole();

	enum Role {
		None,
		User,
		Admin;

		public boolean isAuthenticationRequired() {
			return this != None;
		}
	}

}
