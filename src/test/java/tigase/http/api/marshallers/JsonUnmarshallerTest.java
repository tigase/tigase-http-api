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
package tigase.http.api.marshallers;

import tigase.http.jaxrs.marshallers.JsonMarshaller;
import tigase.http.jaxrs.marshallers.JsonUnmarshaller;
import tigase.http.jaxrs.marshallers.Marshaller;
import tigase.http.jaxrs.marshallers.Unmarshaller;

class JsonUnmarshallerTest
		extends AbstractUnmarshallerTest {

	@Override
	Marshaller createMarshaller() {
		return new JsonMarshaller();
	}

	@Override
	Unmarshaller createUnmarshaller() {
		return new JsonUnmarshaller();
	}
}