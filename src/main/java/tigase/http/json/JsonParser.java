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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JsonParser {

	public static Object parseJson(String text) throws InvalidJsonException {
		return new JsonParser().parse(text);
	}

	public static Object parseJson(byte[] data) throws InvalidJsonException {
	    return parseJson(data, StandardCharsets.UTF_8);
	}

	public static Object parseJson(byte[] data, Charset charset) throws InvalidJsonException {
		return parseJson(new String(data, charset));
	}

	enum State {
		READ_OBJECT,
		READ_KEY,
		READ_VALUE
	}

	private char[] chars;
	private int i = 0;

	public void skipWhitespaces() throws InvalidJsonException {
		while (i < chars.length && Character.isWhitespace(chars[i])) {
			i++;
		}
		if (i == chars.length) {
			throw new InvalidJsonException("Unexpected end of input");
		}
	}

	private List readList() throws InvalidJsonException {
		i++;
		ArrayList list = new ArrayList();
		while (i < chars.length && chars[i] != ']') {
			if (list.isEmpty()) {
				list.add(readValue());
			} else {
				skipWhitespaces();
				if (chars[i] != ',') {
					throw new InvalidJsonException("Got '" + chars[i] + "' but expected ',' at " + i);
				}
				i++;
				list.add(readValue());
				skipWhitespaces();
			}
		}
		i++;
		return list;
	}

	private Object readObject() throws InvalidJsonException {
		skipWhitespaces();
		if (chars[i] != '{') {
			throw new InvalidJsonException("Got '" + chars[i] + "' but expected '{' at " + i);
		}
		i++;
		Map<String, Object> object = new LinkedHashMap<>();
		skipWhitespaces();
		while (i < chars.length && chars[i] != '}') {
			if (!object.isEmpty()) {
				skipWhitespaces();
				if (i < chars.length && chars[i] != ',') {
					throw new InvalidJsonException("Got '" + chars[i] + "' but expected ',' at " + i);
				}
				i++;
			}

			String key = readString();
			skipWhitespaces();
			if (i >= chars.length) {
				throw new InvalidJsonException("Unexpected end of input");
			}
			if (chars[i] != ':') {
				throw new InvalidJsonException("Got '" + chars[i] + "' but expected ':' at " + i);
			}
			i++;
			if (i >= chars.length) {
				throw new InvalidJsonException("Unexpected end of input");
			}
			Object value = readValue();
			object.put(key, value);
			skipWhitespaces();
		}
		i++;
		return object;
	}

	private Object readValue() throws InvalidJsonException {
		skipWhitespaces();
		char c = chars[i];
		return switch (c) {
			case ',' -> throw new InvalidJsonException("Unexpected ','");
			case  '\'', '"' -> readQuotedString(c);
			case '{' -> readObject();
			case '[' -> readList();
			default -> readPrimitiveValue();
		};
	}

	private Object readPrimitiveValue() throws InvalidJsonException {
		String string = readUnquotedString();
		return switch (string) {
			case "true" -> Boolean.TRUE;
			case "false" -> Boolean.FALSE;
			case "null" -> null;
			default -> {
				char c1 = string.charAt(0);
				if (Character.isDigit(c1) || c1 == '-' || c1 == '+') {
					if (string.contains(".")) {
						yield Double.parseDouble(string);
					}
					long value = Long.parseLong(string);
					if (value < Integer.MAX_VALUE) {
						yield (int) value;
					}
					yield value;
				}
				yield string;
			}
		};
	}

	private String readQuotedString(char quoteChar) throws InvalidJsonException {
		i++;
		if (i >= chars.length) {
			throw new InvalidJsonException("Unexpected end of input");
		}

		//int start = i;
		StringBuilder sb = new StringBuilder();
		while (i < chars.length && chars[i] != quoteChar) {
			if (chars[i] == '\\') {
				i++;
				if (i >= chars.length) {
					throw new InvalidJsonException("Unexpected end of input");
				}
				switch (chars[i]) {
					case '\\' -> sb.append('\\');
					case '/' -> sb.append('/');
					case 'b' -> sb.append('\b');
					case 't' -> sb.append('\t');
					case 'r' -> sb.append('\r');
					case 'n' -> sb.append('\n');
					case 'f' -> sb.append('\f');
					case 'u' -> sb.append(decodeU());
					case '\"' -> sb.append("\"");
					case '\'' -> sb.append("\'");
					default -> sb.append(chars[i]);
				};
				i++;
			} else {
				sb.append(chars[i]);
				i++;
			}
		}
		String str = sb.toString();
		i++;
		return str;
	}

	private char decodeU() throws InvalidJsonException {
		i++;
		int start = i;
		int end = i+4;
		i = i + 3;
		if (i >= chars.length) {
			throw new InvalidJsonException("Unexpected end of input");
		}

		return (char) Integer.parseInt(new String(Arrays.copyOfRange(chars, start, end)), 16);
	}

	private String readUnquotedString() throws InvalidJsonException {
		if (i >= chars.length) {
			throw new InvalidJsonException("Unexpected end of input");
		}

		int start = i;
		while (i < chars.length && (!Character.isWhitespace(chars[i])) && chars[i] != ':' && chars[i] != ',' && chars[i] != '}') {
			i++;
		}
		return new String(Arrays.copyOfRange(chars, start, i));
	}

	private String readString() throws InvalidJsonException {
		skipWhitespaces();
		if (i >= chars.length) {
			throw new InvalidJsonException("Unexpected end of input");
		}
		
		char c = chars[i];
		if (c == '\'' || c == '"') {
			return readQuotedString(c);
		} else {
			return readUnquotedString();
		}
	}

	public Object parse(String input) throws InvalidJsonException {
		chars = input.toCharArray();
		i = 0;
		return readObject();
	}

	public static class InvalidJsonException extends Exception {
		public InvalidJsonException(String message) {
			super(message);
		}
	}

}
