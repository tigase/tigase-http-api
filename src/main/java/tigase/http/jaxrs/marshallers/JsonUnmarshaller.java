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

import jakarta.xml.bind.UnmarshalException;
import tigase.http.json.JsonParser;
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

public class JsonUnmarshaller extends AbstractUnmarshaller implements Unmarshaller {

	private static final Map<Class, Function<Object,Object>> DEFAULT_DESERIALIZERS = new HashMap<>();

	static {
		DEFAULT_DESERIALIZERS.put(Long.class, obj -> ((Number) obj).longValue());
		DEFAULT_DESERIALIZERS.put(Double.class, obj -> ((Number) obj).doubleValue());
		DEFAULT_DESERIALIZERS.put(Integer.class, obj -> ((Number) obj).intValue());
		DEFAULT_DESERIALIZERS.put(Float.class, obj -> ((Number) obj).floatValue());
		DEFAULT_DESERIALIZERS.put(UUID.class, obj -> UUID.fromString((String) obj));
		DEFAULT_DESERIALIZERS.put(LocalTime.class, obj -> LocalTime.parse((String) obj));

		DEFAULT_DESERIALIZERS.put(ZonedDateTime.class, str -> ZonedDateTime.parse((String) str, ISO_OFFSET_DATE_TIME));
		DEFAULT_DESERIALIZERS.put(LocalDateTime.class, obj -> LocalDateTime.parse(((String) obj)));
		DEFAULT_DESERIALIZERS.put(LocalDate.class, obj -> LocalDate.parse(((String) obj)));
		DEFAULT_DESERIALIZERS.put(String.class, obj -> obj);
		DEFAULT_DESERIALIZERS.put(Date.class,
								  obj -> Date.from(ZonedDateTime.parse(((String) obj), ISO_OFFSET_DATE_TIME).toInstant()));
		DEFAULT_DESERIALIZERS.put(JID.class, obj -> JID.jidInstanceNS((String) obj));
		DEFAULT_DESERIALIZERS.put(BareJID.class, obj -> BareJID.bareJIDInstanceNS((String) obj));
	}

	private final Map<Class, Function<Object,Object>> DESERIALIZERS = new HashMap<>(DEFAULT_DESERIALIZERS);

	@Override
	public Object unmarshal(Class clazz, Reader inReader) throws UnmarshalException, IOException {
		Map<String, Object> root;
		try (BufferedReader reader = new BufferedReader(inReader)) {
			String inData = reader.lines().collect(Collectors.joining("\n"));
			try {
				root = (Map<String, Object>) new JsonParser().parse(inData);
			} catch (JsonParser.InvalidJsonException e) {
				throw new UnmarshalException("Could not parse JSON: " + inData);
			}
		}

		if (root == null) {
			return null;
		}

		return unmarshal(clazz, root);
	}

	protected Object unmarshal(Class clazz, Map<String, Object> root)
			throws UnmarshalException {
		try {
			Object object = clazz.getDeclaredConstructor().newInstance();

			for (Field field : clazz.getDeclaredFields()) {
				Object value = root.get(field.getName());
				if (value != null) {
					if (Collection.class.isAssignableFrom(field.getType())) {
						if (value instanceof Collection) {
							Collection collection = createCollectionInstance((Class<Collection>) field.getType());
							for (Object item : (Collection) value) {
								Object v = deserializeValue((Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0],
															item);
								if (v != null) {
									collection.add(v);
								}
							}
							setFieldValue(object, field, collection);
						} else {
							throw new UnmarshalException("Invalid value for field " + field.getName());
						}
					} else {
						Object v = deserializeValue(field.getType(), value);
						if (v != null) {
							setFieldValue(object, field, v);
						}
					}
				}
			}

			return object;
		} catch (NoSuchFieldException|InvocationTargetException|InstantiationException|IllegalAccessException|NoSuchMethodException e) {
			throw new UnmarshalException("Could not unmarshal instance of " + clazz.getName(), e);
		}
	}

	protected Object deserializeValue(Class clazz, Object value)
			throws NoSuchFieldException, UnmarshalException, InvocationTargetException, NoSuchMethodException,
				   InstantiationException, IllegalAccessException {
		if (clazz.isAssignableFrom(value.getClass())) {
			return value;
		} else {
			Object v = deserialize(clazz, value);
			if (v != null) {
				return v;
			} else {
				return unmarshal(clazz, (Map<String, Object>) value);
			}
		}
	}

	protected Object deserialize(Class clazz, Object value) throws UnmarshalException {
		Function<Object, Object> serializer = DESERIALIZERS.get(clazz);
		if (serializer == null) {
			return null;
		} else {
			try {
				return serializer.apply(value);
			} catch (ClassCastException ex) {
				throw new UnmarshalException("Failed to convert " + value.getClass() + " to " + clazz.getName(), ex);
			}
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
	
	protected void setFieldValue(Object object, Field field, Object value)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		try {
			Method method = object.getClass().getDeclaredMethod("set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1), field.getType());
			method.invoke(object, value);
		} catch (IllegalArgumentException ex) {
			System.out.println("Could not set " + value + "/" + value.getClass() + " to field " + field.getName());
			throw ex;
		}
	}

}
