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
package tigase.http.modules.rest.docs;

import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.MarshalException;
import tigase.http.jaxrs.marshallers.JsonMarshaller;
import tigase.http.jaxrs.marshallers.Marshaller;
import tigase.http.jaxrs.marshallers.XmlMarshaller;
import tigase.http.jaxrs.utils.JaxRsUtil;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PayloadExample {

	private static final Logger log = Logger.getLogger(PayloadExample.class.getName());

	public static PayloadExample create(Class<?> clazz, String contentType) {
		try {
			Marshaller marshaller = switch (contentType) {
				case MediaType.APPLICATION_JSON -> new JsonMarshaller(2);
				case MediaType.APPLICATION_XML -> new XmlMarshaller(2);
				default -> null;
			};

			if (marshaller == null) {
				return null;
			}

			Object object = getObjectExample(clazz);
			if (object != null) {
				StringWriter writer = new StringWriter();
				marshaller.marshall(object, writer);
				return new PayloadExample(contentType, writer.toString());
			} else {
				return null;
			}
		} catch (MarshalException | IOException ex) {
			// ignoring..
			log.log(Level.WARNING, ex, () -> "could not serialize instace of class " + clazz.getName());
			return null;
		}
	}

	public static List<PayloadExample> create(Class<?> clazz, String[] contentTypes) {
		return Arrays.stream(contentTypes)
				.map(contentType -> PayloadExample.create(clazz, contentType))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private final String id = "data-" + UUID.randomUUID().toString();
	private final String contentType;
	private final String payload;

	public PayloadExample(String contentType, String payload) {
		this.contentType = contentType;
		this.payload = payload;
	}

	public String getId() {
		return id;
	}

	public String getContentType() {
		return contentType;
	}

	public String getPayload() {
		return payload;
	}

	private static Object getObjectExample(Class<?> clazz) {
		try {
			Object object = clazz.getDeclaredConstructor().newInstance();
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				field.set(object, generateExampleValue(field.getGenericType()));
			}
			return object;
		} catch (InvocationTargetException |InstantiationException|IllegalAccessException|NoSuchMethodException ex) {
			log.log(Level.WARNING, ex, () -> "could not create instance of class " + clazz.getName());
			return null;
		}
	}

	private static Object generateExampleValue(Type type)
			throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		if (type instanceof Class<?>) {
			return generateExampleValue((Class) type);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
				Collection collection = JaxRsUtil.createCollectionInstance((Class<Collection>) pt.getRawType());
				for (int i=0; i<3; i++) {
					collection.add(generateExampleValue(pt.getActualTypeArguments()[0]));
				}
				return collection;
			} else {
				return getObjectExample((Class<?>) pt.getRawType());
			}
		} else {
			return null;
		}
	}

	private static Object generateExampleValue(Class clazz) {
		if (String.class.equals(clazz)) {
			return "string";
		} else if (BareJID.class.equals(clazz)) {
			return BareJID.bareJIDInstanceNS("user", "example.com");
		} else if (JID.class.equals(clazz)) {
			return JID.jidInstanceNS("user", "example.com", "resource-1");
		} else if (long.class.equals(clazz) || Long.class.equals(clazz)) {
			return 0l;
		} else if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
			return 0;
		} else if (double.class.equals(clazz) || Double.class.equals(clazz)) {
			return 0.0;
		} else if (float.class.equals(clazz) || Float.class.equals(clazz)) {
			return 0.0;
		} else if (UUID.class.equals(clazz)) {
			return UUID.randomUUID().toString();
		} else if (LocalTime.class.equals(clazz)) {
			return LocalTime.now();
		} else if (LocalDateTime.class.equals(clazz)) {
			return LocalDateTime.now();
		} else if (LocalDate.class.equals(clazz)) {
			return LocalDate.now();
		} else if (ZonedDateTime.class.equals(clazz)) {
			return ZonedDateTime.now();
		} else if (Date.class.equals(clazz)) {
			return new Date();
		} else if (Enum.class.isAssignableFrom(clazz)) {
			return clazz.getEnumConstants()[0];
		} else {
			return getObjectExample(clazz);
		}
	}
}
