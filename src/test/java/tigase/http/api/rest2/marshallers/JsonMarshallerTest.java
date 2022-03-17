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
import org.junit.jupiter.api.Test;
import tigase.http.jaxrs.marshallers.JsonMarshaller;
import tigase.http.jaxrs.marshallers.Marshaller;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

class JsonMarshallerTest
		extends AbstractMarshallerTest {


	@Test
	public void test1() throws MarshalException, IOException {

		TestObject data = new TestObject("root");
		data.getItems().add(new TestObject("item 1"));
		data.getItems().add(new TestObject("item 2"));

		String expected = "{\"title\":\"root\",\"data\":\"" + data.getData() + "\",\"date\":\"" +
				ZonedDateTime.ofInstant(data.getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\",\"items\":[{\"title\":\"item 1\",\"data\":\"" + data.getData() + "\",\"date\":\"" +
				ZonedDateTime.ofInstant(data.getItems().get(0).getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) +
				"\",\"items\":[]},{\"title\":\"item 2\",\"data\":\"" + data.getData() + "\",\"date\":\"" +
				ZonedDateTime.ofInstant(data.getItems().get(1).getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\",\"items\":[]}]}";

		assertMarshalling(expected, data);
	}

	@Test
	public void test2() throws MarshalException, IOException {

		TestObject data = new TestObject("root");
		data.getItems().add(new TestObject("item 1"));
		data.getItems().add(new TestObject("item 2"));

		String expected = "{\n  \"title\":\"root\",\n  \"data\":\"" + data.getData() + "\",\n  \"date\":\"" +
				ZonedDateTime.ofInstant(data.getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\",\n  \"items\":[\n    {\n      \"title\":\"item 1\",\n      \"data\":\"" + data.getData() + "\",\n      \"date\":\"" +
				ZonedDateTime.ofInstant(data.getItems().get(0).getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) +
				"\",\n      \"items\":[]\n    },\n    {\n      \"title\":\"item 2\",\n      \"data\":\"" + data.getData() + "\",\n      \"date\":\"" +
				ZonedDateTime.ofInstant(data.getItems().get(1).getDate().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\",\n      \"items\":[]\n    }\n  ]\n}";

		assertMarshallingPretty(expected, data);
	}

	@Override
	Marshaller createMarshaller() {
		return new JsonMarshaller();
	}

	@Override
	Marshaller createMarshallerPretty() {
		return new JsonMarshaller(2);
	}

}