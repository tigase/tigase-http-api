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

import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import tigase.xml.*;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class XmlUnmarshaller extends AbstractUnmarshaller implements Unmarshaller {

	private static final String DEFAULT = "##default";
	private static final SimpleParser PARSER = SingletonFactory.getParserInstance();

	private static final Map<Class,Function<String,Object>> DEFAULT_DESERIALIZERS = new HashMap<>();

	static {
		DEFAULT_DESERIALIZERS.put(Long.class, Long::parseLong);
		DEFAULT_DESERIALIZERS.put(Double.class, Double::parseDouble);
		DEFAULT_DESERIALIZERS.put(Integer.class, Integer::parseInt);
		DEFAULT_DESERIALIZERS.put(Float.class, Float::parseFloat);
		DEFAULT_DESERIALIZERS.put(UUID.class, UUID::fromString);
		DEFAULT_DESERIALIZERS.put(LocalTime.class, LocalTime::parse);

		DEFAULT_DESERIALIZERS.put(ZonedDateTime.class, str -> ZonedDateTime.parse(str, ISO_OFFSET_DATE_TIME));
		DEFAULT_DESERIALIZERS.put(LocalDateTime.class, LocalDateTime::parse);
		DEFAULT_DESERIALIZERS.put(LocalDate.class, LocalDate::parse);
		DEFAULT_DESERIALIZERS.put(String.class, obj -> obj);
		DEFAULT_DESERIALIZERS.put(Date.class,
								  str -> Date.from(ZonedDateTime.parse(str, ISO_OFFSET_DATE_TIME).toInstant()));
		DEFAULT_DESERIALIZERS.put(JID.class, JID::jidInstanceNS);
		DEFAULT_DESERIALIZERS.put(BareJID.class, BareJID::bareJIDInstanceNS);
	}

	private final Map<Class,Function<String, Object>> DESERIALIZERS = new HashMap<>(DEFAULT_DESERIALIZERS);
	
	public static boolean isNotDefault(String value) {
		return !DEFAULT.equals(value);
	}


	//@Override
	public Object unmarshal(Class clazz, Reader inReader) throws UnmarshalException, IOException {
		DomBuilderHandler handler = new DomBuilderHandler();
		Element root = null;
		try (BufferedReader reader = new BufferedReader(inReader)) {
			String inData = reader.lines().collect(Collectors.joining("\n"));
			PARSER.parse(handler, inData);
			root = handler.getParsedElements().poll();
			if (root == null) {
				throw new UnmarshalException("Could not parse XML: " + inData);
			}
		}

		XmlRootElement xmlRootElement = (XmlRootElement) clazz.getAnnotation(XmlRootElement.class);
		String name = Optional.ofNullable(xmlRootElement)
				.map(XmlRootElement::name)
				.filter(XmlUnmarshaller::isNotDefault)
				.orElse(clazz.getSimpleName());
		Optional<String> namespace = Optional.ofNullable(xmlRootElement)
				.map(XmlRootElement::namespace)
				.filter(XmlUnmarshaller::isNotDefault);
		return unmarshal(clazz, name, namespace, root);
	}

	protected Object unmarshal(Class clazz, String name, Optional<String> namespace, Element root)
			throws UnmarshalException {
		if (!name.equals(root.getName())) {
			throw new UnmarshalException("Invalid element name, expected " + name + ", got " + root.getName());
		}
		if (namespace.isPresent()) {
			if (namespace.filter(ns -> ns.equals(root.getXMLNS())).isEmpty()) {
				throw new UnmarshalException(
						"Invalid element namespace, expected " + namespace.get() + ", got " + root.getXMLNS());
			}
		}

		try {
			Object object = clazz.getDeclaredConstructor().newInstance();

			Field[] fields = clazz.getDeclaredFields();
			List<Field> attributeFields = Arrays.stream(fields)
					.filter(field -> field.getAnnotation(XmlAttribute.class) != null)
					.collect(Collectors.toList());
			for (Field field : attributeFields) {
				String valueStr = root.getAttribute(Optional.ofNullable(field.getAnnotation(XmlAttribute.class))
															.map(XmlAttribute::name)
															.filter(XmlMarshaller::isNotDefault)
															.orElse(field.getName()));
				if (valueStr != null) {
					Object value = deserialize(field.getClass(), valueStr);
					if (value != null) {
						setFieldValue(object, field, value);
					}
				}
			}

			List<Field> elementFields = Arrays.stream(fields)
					.filter(field -> field.getAnnotation(XmlAttribute.class) == null)
					.collect(Collectors.toList());
			for (Field field : elementFields) {
				String fieldName = Optional.ofNullable(field.getAnnotation(XmlAttribute.class))
						.map(XmlAttribute::name)
						.filter(XmlMarshaller::isNotDefault)
						.orElse(field.getName());

				if (Collection.class.isAssignableFrom(field.getType())) {
					List<Element> elems = root.findChildren(el -> fieldName.equals(el.getName()));
					if (elems != null) {

						Collection collection = createCollectionInstance((Class<Collection>) field.getType());
						for (Element elem : elems) {
							Object value = deserializeValue((Class) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0], fieldName, elem);
							if (value != null) {
								collection.add(value);
							}
						}
						setFieldValue(object, field, collection);
					}
				} else {
					Element elem = root.findChild(el -> fieldName.equals(el.getName()));
					if (elem != null) {
						Object value = deserializeValue(field.getType(), fieldName, elem);
						if (value != null) {
							setFieldValue(object, field, value);
						}
					}
				}
			}

			return object;
		} catch (NoSuchFieldException|InvocationTargetException|InstantiationException|IllegalAccessException|NoSuchMethodException e) {
			throw new UnmarshalException("Could not unmarshal instance of " + clazz.getName(), e);
		}
	}

	protected Collection createCollectionInstance(Class<Collection> collectionClass)
			throws UnmarshalException, NoSuchMethodException, InvocationTargetException, InstantiationException,
				   IllegalAccessException {
		if (Modifier.isAbstract(collectionClass.getModifiers()) || Modifier.isInterface(collectionClass.getModifiers())) {
			if (List.class.isAssignableFrom(collectionClass)) {
				return new ArrayList();
			} else if (Set.class.isAssignableFrom(collectionClass)) {
				return new HashSet();
			} else {
				throw new UnmarshalException("Unsupported collection class " + collectionClass.getName());
			}
		} else {
			return collectionClass.getDeclaredConstructor().newInstance();
		}
	}

	protected Object deserializeValue(Class clazz, String name, Element elem)
			throws NoSuchFieldException, UnmarshalException {
		String valueStr = XMLUtils.unescape(elem.getCData());
		if (valueStr != null) {
			Object value = deserialize(clazz, valueStr);
			if (value != null) {
				return value;
			}
		}

		Field field = clazz.getDeclaredField(name);
		Optional<String> namespace = Optional.ofNullable(field.getAnnotation(XmlElement.class))
				.map(XmlElement::namespace)
				.filter(XmlMarshaller::isNotDefault)
				.or(() -> Optional.ofNullable(field.getClass().getAnnotation(XmlRootElement.class))
						.map(XmlRootElement::namespace)
						.filter(XmlMarshaller::isNotDefault));
		return unmarshal(clazz, name, namespace, elem);
	}

	protected Object deserialize(Class clazz, String valueStr) {
		Function<String, Object> serializer = DESERIALIZERS.get(clazz);
		if (serializer == null) {
			return null;
		} else {
			return serializer.apply(valueStr);
		}
	}

	protected void setFieldValue(Object object, Field field, Object value)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Method method = object.getClass().getDeclaredMethod("set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1), field.getType());
		method.invoke(object, value);
	}
	
}
