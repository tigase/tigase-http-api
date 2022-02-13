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
package tigase.http.json;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonSerializer {

	public JsonSerializer() {
	}

	public String serialize(Object value) {
		if (value instanceof Number) {
			return value.toString();
		} else if (value instanceof List) {
			return "[" + ((List) value).stream().map(this::serialize).collect(Collectors.joining(",")) + "]";
		} else if (value instanceof Map) {
			return "{" + ((Stream<Map.Entry>) ((Map) value).entrySet().stream()).map(
							(Map.Entry e) -> "\"" + e.getKey() + "\" : " + serialize(e.getValue()))
					.collect(Collectors.joining(",")) + "}";
		} else if (value != null) {
			return escapeString(value.toString());
		} else {
			return "null";
		}
	}

	public static String escapeString(String in) {
		return "\"" + in.replace("\"", "\\\"")
				.replace("\'", "\\\'")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\b", "\\b")
				.replace("\t", "\\t") + "\"";
	}

}
