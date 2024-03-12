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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

class XmlMarshallerTest
		extends AbstractMarshallerTest {

	@Test
	public void test1() throws MarshalException, IOException {

		TestObject data = new TestObject("root");
		data.getItems().add(new TestObject("item 1"));
		data.getItems().add(new TestObject("item 2"));

		String expected = "<TestObject data=\"" + data.getData() + "\"><title>root</title><date>" +
				ZonedDateTime.ofInstant(data.getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "</date><items data=\"" +
				data.getData() + "\"><title>item 1</title><date>" +
				ZonedDateTime.ofInstant(data.getItems().get(0).getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) +
				"</date></items><items data=\"" + data.getData() + "\"><title>item 2</title><date>" +
				ZonedDateTime.ofInstant(data.getItems().get(1).getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "</date></items></TestObject>";
																													  
		assertMarshalling(expected, data);
	}

	@Override
	Marshaller createMarshaller() {
		return new XmlMarshaller();
	}
}