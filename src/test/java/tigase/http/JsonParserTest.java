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
package tigase.http;

import groovy.json.JsonBuilder;
import org.junit.Test;
import tigase.http.json.JsonParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonParserTest {

	@Test
	public void parse() throws JsonParser.InvalidJsonException {
		Map<String,Object> input = new HashMap<>();
		input.put("number", 1);
		input.put("string", "Ala\n\t ma \"kota\uD83D\uDC69\u200D✈️ \\\' \'");
		input.put("bool", true);
		input.put("null", null);
		Object obj = new HashMap<>();
		input.put("object", obj);
		Map<String,Object> obj2 = new HashMap<>();
		obj2.put("test1", "123");
		obj2.put("test2", 123);
		obj2.put("test3", -123);
		obj2.put("test4", 123.3);
		obj2.put("test5", -10e10);
		input.put("object2", obj2);
		input.put("list", List.of(1, -1.2, "test", new HashMap<>()));

		String objectString = new JsonBuilder(input).toPrettyString();
		System.out.println("input : " + objectString);
		Object result = new JsonParser().parse(objectString);
		String objectString2 = new JsonBuilder(result).toPrettyString();
		System.out.println("output: " + objectString2);
	}
}