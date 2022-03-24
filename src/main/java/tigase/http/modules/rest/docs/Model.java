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

import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.validation.constraints.NotNull;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class Model {

	public static Model create(Class clazz) {
		List<Model.Field> fields = new ArrayList<>();
		for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
			Class type = findActualClass(field.getGenericType());
			if (type != null) {
				boolean isCollection = Collection.class.isAssignableFrom(type);
				Model model = (type.getPackageName().startsWith("java.") || type.equals(clazz) || type.equals(BareJID.class) || type.equals(JID.class) || Enum.class.isAssignableFrom(type))
							  ? null
							  : create(type);
				boolean isRequired = field.getAnnotation(NotNull.class) != null;
				fields.add(new Model.Field(field.getName(), isRequired, isCollection, type, model));
			}
		}
		return new Model(clazz.getSimpleName(),fields);
	}

	private static Class findActualClass(Type type) {
		if (type instanceof Class<?>) {
			return (Class) type;
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
				return findActualClass(pt.getActualTypeArguments()[0]);
			} else {
				return findActualClass(pt.getRawType());
			}
		} else {
			return null;
		}
	}

	private final String id = "id-" + UUID.randomUUID().toString();
	private final String name;
	private final List<Field> fields;

	public Model(String name, List<Field> fields) {
		this.name = name;
		this.fields = fields;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Field> getFields() {
		return fields;
	}

	public static class Field {

		private final String name;
		private final Class clazz;
		private final Model model;
		private final boolean collection;
		private final boolean required;

		public Field(String name, boolean required, boolean collection, Class clazz, Model model) {
			this.name = name;
			this.clazz = clazz;
			this.model = model;
			this.required = required;
			this.collection = collection;
		}

		public String getName() {
			return name;
		}

		public boolean isRequired() {
			return required;
		}

		public Class getClazz() {
			return clazz;
		}

		public String getTypeString() {
			if (isCollection()) {
				return "Collection<" + clazz.getSimpleName() + ">";
			}
			return clazz.getSimpleName();
		}

		public boolean isCollection() {
			return collection;
		}

		public Model getModel() {
			return model;
		}

		public List<String> getPossibleValues() {
			Object[] arr = clazz.getEnumConstants();
			if (arr == null) {
				return null;
			}
			return Arrays.stream(arr).map(Enum.class::cast).map(Enum::name).collect(Collectors.toList());
		}
	}

}
