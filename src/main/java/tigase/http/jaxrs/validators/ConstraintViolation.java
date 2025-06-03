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
package tigase.http.jaxrs.validators;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

public class ConstraintViolation {
	
	public ConstraintViolation(String messageTemplate, Path path, AnnotatedElement store, Object invalidValue, String parameterName) {
		this.messageTemplate = messageTemplate;
		this.path = path;
		this.store = store;
		this.invalidValue = invalidValue;
		this.parameterName = parameterName;
	}

	public static record Path(String value) {
		public static Path ROOT = new Path("");

		public Path appendField(String node) {
			return new Path(value + "." + node);
		}

		public Path appendItem(int position) {
			return new Path(value + "[" + position + "]");
		}
	}

	private final String parameterName;
	private final String messageTemplate;
	private final Path path;
	private final AnnotatedElement store;
	private final Object invalidValue;

	public String getMessage() {
		if (store instanceof Parameter) {
			return (parameterName != null ? parameterName : ("Parameter '" + ((Parameter) store).getName() + "'")) + " " + messageTemplate + ", received: '" + invalidValue + "'";
		} else if (store instanceof Field) {
			return "Field '" + path.value() + "' " + messageTemplate + ", received: '" + invalidValue + "'";
		} else {
			return (path == null) ? "" : path.value() + " " + messageTemplate + ", received: '" + invalidValue + "'";
		}
	}

}
