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

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.RuntimeDelegate;
import tigase.http.jaxrs.ResponseImpl;

public class RuntimeDelegateImpl extends RuntimeDelegate {

	public RuntimeDelegateImpl() {
	}

	@Override
	public UriBuilder createUriBuilder() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public Response.ResponseBuilder createResponseBuilder() {
		return new ResponseImpl.ResponseBuilderImpl();
	}

	@Override
	public Variant.VariantListBuilder createVariantListBuilder() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public <T> T createEndpoint(Application application, Class<T> aClass)
			throws IllegalArgumentException, UnsupportedOperationException {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> aClass) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public Link.Builder createLinkBuilder() {
		throw new UnsupportedOperationException("Feature not implemented");
	}
}
