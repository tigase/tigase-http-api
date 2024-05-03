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
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class UriInfoImpl implements UriInfo {

	private final String baseUri;
	private final HttpServletRequest request;

	public UriInfoImpl(HttpServletRequest request, String baseUri) {
		this.request = request;
		this.baseUri = baseUri;
	}

	@Override
	public String getPath() {
		return getPath(true);
	}

	@Override
	public String getPath(boolean decode) {
		if (!decode) {
			throw new RuntimeException("Encoded path is not supported!");
		}

//		String path = request.getPathInfo();
//		String prefix = request.getServletContext().getContextPath();
//		if (prefix == null || prefix.isEmpty() || prefix.equals("/")) {
//			return path;
//		}

		return request.getPathInfo();
	}

	@Override
	public List<PathSegment> getPathSegments() {
		throw new RuntimeException("Path segments are not supported!");
	}

	@Override
	public List<PathSegment> getPathSegments(boolean b) {
		throw new RuntimeException("Path segments are not supported!");
	}

	@Override
	public URI getRequestUri() {
		return getRequestUriBuilder().build();
	}

	@Override
	public UriBuilder getRequestUriBuilder() {
		UriBuilder builder = UriBuilder.fromPath(request.getRequestURI());
		for (Map.Entry<String,String[]> e : request.getParameterMap().entrySet()) {
	        builder.replaceQueryParam(e.getKey(), (Object[]) e.getValue());
		}
		return builder;
	}

	@Override
	public URI getAbsolutePath() {
		return getAbsolutePathBuilder().build();
	}

	@Override
	public UriBuilder getAbsolutePathBuilder() {
		return getRequestUriBuilder().scheme(request.getScheme())
				.host(request.getServerName())
				.port(request.getLocalPort());
	}

	@Override
	public URI getBaseUri() {
		return getBaseUriBuilder()
				.build();
	}

	@Override
	public UriBuilder getBaseUriBuilder() {
		return UriBuilder.newInstance()
				.scheme(request.getScheme())
				.host(request.getServerName())
				.port(request.getServerPort())
				.path(baseUri);
	}

	@Override
	public MultivaluedMap<String, String> getPathParameters() {
		throw new RuntimeException("Path parameters are not supported!");
	}

	@Override
	public MultivaluedMap<String, String> getPathParameters(boolean b) {
		throw new RuntimeException("Path parameters are not supported!");
	}

	@Override
	public MultivaluedMap<String, String> getQueryParameters() {
		return MultivaluedMapImpl.fromArrayMap(request.getParameterMap());
	}

	@Override
	public MultivaluedMap<String, String> getQueryParameters(boolean b) {
		return getQueryParameters();
	}

	@Override
	public List<String> getMatchedURIs() {
		throw new RuntimeException("Matched URIs are not supported!");
	}

	@Override
	public List<String> getMatchedURIs(boolean b) {
		return getMatchedURIs();
	}

	@Override
	public List<Object> getMatchedResources() {
		throw new RuntimeException("Matched resources are not supported!");
	}

	@Override
	public URI resolve(URI uri) {
		return getBaseUri().resolve(uri);
	}

	@Override
	public URI relativize(URI uri) {
		throw new RuntimeException("Relativize is not supported!");
	}
}
