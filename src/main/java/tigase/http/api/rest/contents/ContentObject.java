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
package tigase.http.api.rest.contents;

import java.util.HashMap;
import java.util.Map;

public final class ContentObject implements Content {

	private final Map<String, Object> object;

	public ContentObject() {
		this(new HashMap<>());
	}

	public ContentObject(Map<String, Object> object) {
		this.object = object;
	}

	public <T> T get(String field) {
		return (T) this.object.get(field);
	}

	public ContentObject put(String field, Object value) {
		if (value == null) {
			this.object.remove(field);
		} else {
			this.object.put(field, value);
		}
		return this;
	}

	public Map<String, Object> getObject() {
		return object;
	}

}
