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

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class ContainerRequestContext {

	private static ThreadLocal<ContainerRequestContext> contexts = new ThreadLocal<>();

	public static void setContext(ContainerRequestContext context) {
		contexts.set(context);
	}
	
	public static void resetContext() {
		contexts.remove();
	}

	public static ContainerRequestContext getContext() {
		return contexts.get();
	}

	private final HttpServletRequest request;
	private MultivaluedMap<String, String> headers;
	private SecurityContext securityContext;
	private UriInfo uriInfo;

	public ContainerRequestContext(HttpServletRequest request) {
		this.request = request;
	}

	public MultivaluedMap<String, String> getHeaders() {
		if (headers == null) {
			headers = new MultivaluedMapImpl<>();
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String name = headerNames.nextElement();
				headers.putSingle(name, request.getHeader(name));
			}
		}
		return this.headers;
	}

	public String getHeaderString(String name) {
		return request.getHeader(name);
	}

	public HttpServletRequest getRequest() {
		return request;
	}
	
	public SecurityContext getSecurityContext() {
		if (securityContext == null) {
			securityContext = new SecurityContextImpl(request);
		}
		return securityContext;
	}

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public UriInfo getUriInfo() {
		if (uriInfo == null) {
			String basePath = request.getContextPath();
			String servletPath = request.getServletPath();
			if (!servletPath.isEmpty()) {
				if (basePath.endsWith("/") && servletPath.startsWith("/")) {
					basePath += servletPath.substring(1);
				}
			}
			uriInfo = new UriInfoImpl(request, basePath);
		}
		return uriInfo;
	}
}
