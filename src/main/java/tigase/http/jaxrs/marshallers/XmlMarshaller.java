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
package tigase.http.jaxrs.marshallers;

import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import tigase.xml.XMLUtils;
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
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class XmlMarshaller extends AbstractMarshaller implements Marshaller {

	private static final String DEFAULT = "##default";

	private static final Map<Class,Function<Object,String>> DEFAULT_SERIALIZERS = new HashMap<>();

	static {
		List.of(Long.class, Double.class, Integer.class, Float.class, UUID.class, LocalTime.class)
				.forEach(clazz -> DEFAULT_SERIALIZERS.put(clazz, obj -> obj.toString()));
		List.of(ZonedDateTime.class)
				.forEach(clazz -> DEFAULT_SERIALIZERS.put(clazz,
														  obj -> ((ZonedDateTime) obj).format(ISO_OFFSET_DATE_TIME)));
		List.of(LocalDateTime.class, LocalDate.class)
				.forEach(clazz -> DEFAULT_SERIALIZERS.put(clazz, obj -> obj.toString()));
		DEFAULT_SERIALIZERS.put(String.class, obj -> (String) obj);
		DEFAULT_SERIALIZERS.put(Date.class,
								obj -> ZonedDateTime.ofInstant(((Date) obj).toInstant(), ZoneId.systemDefault())
										.format(ISO_OFFSET_DATE_TIME));
		DEFAULT_SERIALIZERS.put(JID.class, obj -> obj.toString());
		DEFAULT_SERIALIZERS.put(BareJID.class, obj -> obj.toString());

	}

	private final Map<Class,Function<Object, String>> SERIALIZERS = new HashMap<>(DEFAULT_SERIALIZERS);

	public static boolean isNotDefault(String value) {
		return !DEFAULT.equals(value);
	}

	@Override
	public void marshall(Object object, OutputStream outputStream) throws MarshalException, IOException {
		try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
			marshall(object, writer);
		}
	}

	@Override
	public void marshall(Object object, Writer writer)
			throws IOException, MarshalException {
		Class clazz = object.getClass();
		try {
			XmlRootElement xmlRootElement = (XmlRootElement) clazz.getAnnotation(XmlRootElement.class);
			String name = Optional.ofNullable(xmlRootElement)
					.map(XmlRootElement::name)
					.filter(XmlMarshaller::isNotDefault)
					.orElse(clazz.getSimpleName());
			Optional<String> namespace = Optional.ofNullable(xmlRootElement).map(XmlRootElement::namespace).filter(XmlMarshaller::isNotDefault);

			marshall(object, name, namespace, writer);
		} catch (InvocationTargetException|NoSuchMethodException|IllegalAccessException e) {
			throw new MarshalException("Could not marshal instance of " + clazz.getName(), e);
		}
	}

	public void marshall(Object object, String name, Optional<String> namespace, Writer writer)
			throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		writer.write("<");
		writer.write(name);
		Class clazz = object.getClass();
		if (namespace.isPresent()) {
			writer.write(" xmlns=\"");
			writer.write(namespace.get());
			writer.write("\"");
		}
		Field[] fields = clazz.getDeclaredFields();
		List<Field> attributeFields = Arrays.stream(fields)
				.filter(field -> field.getAnnotation(XmlAttribute.class) != null)
				.collect(Collectors.toList());
		for (Field field : attributeFields) {
			Object value = getFieldValue(object, field);

			if (value != null) {
				String valueStr = serialize(value);
				if (valueStr != null) {
					writer.write(" ");
					writer.write(Optional.ofNullable(field.getAnnotation(XmlAttribute.class))
										 .map(XmlAttribute::name)
										 .filter(XmlMarshaller::isNotDefault)
										 .orElse(field.getName()));
					writer.write("=\"");
					writer.write(XMLUtils.escape(valueStr));
					writer.write("\"");
				}
			}
		}

		List<Field> elementFields = Arrays.stream(fields)
				.filter(field -> field.getAnnotation(XmlAttribute.class) == null)
				.collect(Collectors.toList());

		if (elementFields.isEmpty()) {
			writer.write("/>");
		} else {
			writer.write(">");
			
			for (Field field : elementFields) {
				Object value = getFieldValue(object, field);
				if (value != null) {
					if (value instanceof Collection) {
						for (Object item : (Collection) value) {
							serializeValue(field, item, writer);
						}
					} else {
						serializeValue(field, value, writer);
					}
				}
			}

			writer.write("</");
			writer.write(name);
			writer.write(">");
		}
	}

	protected void serializeValue(Field field, Object value, Writer writer)
			throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		String fieldName = Optional.ofNullable(field.getAnnotation(XmlElement.class))
				.map(XmlElement::name)
				.filter(XmlMarshaller::isNotDefault)
				.orElse(field.getName());

		String valueStr = serialize(value);
		if (valueStr == null) {
			marshall(value, fieldName, Optional.ofNullable(field.getAnnotation(XmlElement.class))
					.map(XmlElement::namespace)
					.filter(XmlMarshaller::isNotDefault)
					.or(() -> Optional.ofNullable(field.getClass().getAnnotation(XmlRootElement.class))
							.map(XmlRootElement::namespace)
							.filter(XmlMarshaller::isNotDefault)), writer);
		} else {
			writer.write("<");
			writer.write(fieldName);
			writer.write(">");
			writer.write(XMLUtils.escape(valueStr));
			writer.write("</");
			writer.write(fieldName);
			writer.write(">");
		}
	}

	protected String serialize(Object value) {
		Function<Object, String> serializer = SERIALIZERS.get(value.getClass());
		if (serializer == null) {
			if (value instanceof Enum<?>) {
				return ((Enum<?>) value).name();
			}
			return null;
		} else {
			return serializer.apply(value);
		}
	}
	
}
