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
package tigase.http.jaxrs;

import jakarta.ws.rs.core.AbstractMultivaluedMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MultivaluedMapImpl<V> extends AbstractMultivaluedMap<String,V> {

	public MultivaluedMapImpl() {
		this(new TreeMap(String.CASE_INSENSITIVE_ORDER));
	}

	public MultivaluedMapImpl(Map<String, List<V>> store) {
		super(store);
	}
	
	public static <V> MultivaluedMapImpl<V> fromArrayMap(Map<String, V[]> data) {
		return new MultivaluedMapImpl<>(data.entrySet()
												.stream()
												.collect(Collectors.toMap(Map.Entry::getKey,
																		  e -> Arrays.asList(e.getValue()))));
	}

}
