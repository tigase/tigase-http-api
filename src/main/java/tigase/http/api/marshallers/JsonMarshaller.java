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
import tigase.http.json.JsonSerializer;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Function;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class JsonMarshaller extends AbstractMarshaller implements Marshaller {

	private static final Map<Class, Function<Object,Object>> DEFAULT_SERIALIZERS = new HashMap<>();

	static {
		List.of(Long.class, Double.class, Integer.class, Float.class, UUID.class, LocalTime.class)
				.forEach(clazz -> DEFAULT_SERIALIZERS.put(clazz, obj -> obj.toString()));
		List.of(ZonedDateTime.class)
				.forEach(clazz -> DEFAULT_SERIALIZERS.put(clazz,
														  obj -> ((ZonedDateTime) obj).format(ISO_OFFSET_DATE_TIME)));
		List.of(LocalDateTime.class, LocalDate.class)
				.forEach(clazz -> DEFAULT_SERIALIZERS.put(clazz, obj -> obj.toString()));
		DEFAULT_SERIALIZERS.put(Date.class,
								obj -> ZonedDateTime.ofInstant(((Date) obj).toInstant(), ZoneId.systemDefault()));
		DEFAULT_SERIALIZERS.put(JID.class, obj -> obj.toString());
		DEFAULT_SERIALIZERS.put(BareJID.class, obj -> obj.toString());
	}

	private final Map<Class, Function<Object,Object>> SERIALIZERS = new HashMap<>(DEFAULT_SERIALIZERS);

	@Override
	public void marshall(Object object, OutputStream outputStream) throws MarshalException, IOException {
		try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
			marshall(object, writer);
		}
	}

	@Override
	public void marshall(Object object, Writer writer) throws IOException, MarshalException {
		writer.write("{");
		Class clazz = object.getClass();
		try {
			boolean first = true;
			for (Field field : clazz.getDeclaredFields()) {
				Object value = getFieldValue(object, field);
				if (value != null) {
					if (!first) {
						writer.write(",");
					} else {
						first = false;
					}
					writer.write(JsonSerializer.escapeString(field.getName()));
					writer.write(":");

					serializeValue(value, writer);
				}
			}
			writer.write("}");
		} catch (InvocationTargetException|NoSuchMethodException|IllegalAccessException e) {
			throw new MarshalException("Could not marshal instance of " + clazz.getName(), e);
		}
	}

	public void serializeValue(Object value, Writer writer) throws IOException, MarshalException {
		if (value instanceof Collection) {
			writer.write("[");
			boolean first = true;
			for (Object item : (Collection) value) {
				if (!first) {
					writer.write(",");
				} else {
					first = false;
				}
				//marshall(item, writer);
				serializeValue(item, writer);
			}
			writer.write("]");
		} else if (value instanceof Boolean || value instanceof Long || value instanceof Integer || value instanceof Double || value instanceof Float) {
			writer.write(value.toString());
		} else {
			Function<Object, Object> mapper;
			while ((mapper = SERIALIZERS.get(value.getClass())) != null) {
	            value = mapper.apply(value);
			}

			if (value instanceof String) {
				writer.write(JsonSerializer.escapeString(value.toString()));
			} else {
				marshall(value, writer);
			}
		}
	}

}
