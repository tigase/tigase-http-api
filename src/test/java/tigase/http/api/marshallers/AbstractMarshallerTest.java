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

import jakarta.xml.bind.MarshalException;
import tigase.http.jaxrs.marshallers.Marshaller;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class AbstractMarshallerTest {

	abstract Marshaller createMarshaller();

	protected void assertMarshalling(String expected, Object object) throws MarshalException, IOException {
		Marshaller marshaller = createMarshaller();

		StringBuilderWriter writer = new StringBuilderWriter();

		marshaller.marshall(object, writer);

		assertEquals(expected, writer.toString(), "Marshalling returned unexpected result!");
	}

}