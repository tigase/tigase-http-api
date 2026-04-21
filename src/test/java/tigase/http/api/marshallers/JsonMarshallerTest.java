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
import tigase.http.jaxrs.marshallers.JsonMarshaller;
import tigase.http.jaxrs.marshallers.Marshaller;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JsonMarshallerTest
		extends AbstractMarshallerTest {


	@Test
	public void testSerializeObject() throws MarshalException, IOException {

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
	public void testList() throws MarshalException, IOException {
		List<String> list = new ArrayList<>();
		list.add("item 1");
		list.add("item 2");
		list.add("item 3");
		list.add("item 4");

		String expected ="[\"item 1\",\"item 2\",\"item 3\",\"item 4\"]";

		assertMarshalling(expected, list);
	}



	@Test
	public void testMap() throws MarshalException, IOException {
		Map<String, Integer> map = new HashMap<>();
		map.put("2026-01-01", 100);
		map.put("2026-01-02", 200);
		map.put("2026-01-03", 300);

		String expected = "{\"2026-01-03\":300,\"2026-01-02\":200,\"2026-01-01\":100}";

		assertMarshalling(expected, map);
	}



	@Override
	Marshaller createMarshaller() {
		return new JsonMarshaller();
	}

}