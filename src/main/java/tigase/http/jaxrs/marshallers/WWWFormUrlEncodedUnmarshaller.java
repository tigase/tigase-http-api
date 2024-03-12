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

import jakarta.ws.rs.FormParam;
import jakarta.xml.bind.UnmarshalException;
import tigase.http.jaxrs.JaxRsRequestHandler;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;

public class WWWFormUrlEncodedUnmarshaller {

	public Object unmarshal(Class clazz, HttpServletRequest request) throws UnmarshalException {
		try {
			Object object = clazz.getDeclaredConstructor().newInstance();
			for (Field field : clazz.getDeclaredFields()) {
				FormParam formParam = field.getAnnotation(FormParam.class);
				if (formParam != null) {
					String[] valuesStr = request.getParameterValues(formParam.value());
					Object value = null;
					if (boolean.class.equals(field.getType())) {
						value = valuesStr != null && valuesStr.length == 1 && "on".equals(valuesStr[0]);
					} else {
						if (valuesStr != null) {
							value = JaxRsRequestHandler.convertToValue(field.getGenericType(), valuesStr);
						}
					}
					field.setAccessible(true);
					field.set(object, value);
				}
			}
			return object;
		} catch (Throwable ex) {
			throw new UnmarshalException("Could not decode " + clazz.getCanonicalName() + " from submitted form");
		}
	}

}