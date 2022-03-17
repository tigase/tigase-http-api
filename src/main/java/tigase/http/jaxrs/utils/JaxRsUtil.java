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
package tigase.http.jaxrs.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public class JaxRsUtil {

	public static Collection createCollectionInstance(Class<Collection> collectionClass)
			throws NoSuchMethodException, InvocationTargetException, InstantiationException,
				   IllegalAccessException {
		if (Modifier.isAbstract(collectionClass.getModifiers()) || Modifier.isInterface(collectionClass.getModifiers())) {
			if (List.class.isAssignableFrom(collectionClass)) {
				return new ArrayList();
			} else if (Set.class.isAssignableFrom(collectionClass)) {
				return new HashSet();
			} else {
				throw new InstantiationException("Unsupported collection type: " + collectionClass.getName());
			}
		} else {
			return collectionClass.getDeclaredConstructor().newInstance();
		}
	}
	
}
