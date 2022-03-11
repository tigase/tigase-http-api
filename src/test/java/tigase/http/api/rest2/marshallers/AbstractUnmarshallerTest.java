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
package tigase.http.api.rest2.marshallers;

import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.UnmarshalException;
import org.junit.jupiter.api.Test;
import tigase.http.jaxrs.marshallers.Marshaller;
import tigase.http.jaxrs.marshallers.Unmarshaller;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractUnmarshallerTest {

	abstract Marshaller createMarshaller();
	abstract Unmarshaller createUnmarshaller();

	@Test
	public void assertUnmarshalling() throws MarshalException, IOException, UnmarshalException {
		TestObject object = new TestObject("root");
		object.getItems().add(new TestObject("item 1"));
		object.getItems().add(new TestObject("item 2"));

		StringBuilderWriter writer = new StringBuilderWriter();
		createMarshaller().marshall(object, writer);
		String string = writer.toString();

		Object result = createUnmarshaller().unmarshal(object.getClass(), new StringReader(string));
		assertEquals(object, result, "Unmarshalled object does not match the source object!");
	}

}
