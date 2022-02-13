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

import tigase.http.json.JsonSerializer;
import tigase.xml.Element;
import tigase.xml.XMLUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ContentObjectEncoder {

	public byte[] encodeToXML(ContentObject object) {
		return objectToElement("root", object.getObject()).toString().getBytes(StandardCharsets.UTF_8);
	}

	protected Element objectToElement(String name, Object object) {
		Element el = new Element(name);
		if (object instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) object;
			for (Map.Entry<String, Object> e : map.entrySet()) {
				el.addChild(objectToElement(e.getKey(), e.getValue()));
			}
		} else if (object instanceof List) {
			List list = (List) object;
			for (Object it : list) {
				el.addChild(objectToElement("item", it));
			}
		} else if (object != null) {
			el.setCData(XMLUtils.escape(object.toString()));
		}
		return el;
	}

	public byte[] encodeToJSON(ContentObject object) {
		return new JsonSerializer().serialize(object).getBytes(StandardCharsets.UTF_8);
	}

}
