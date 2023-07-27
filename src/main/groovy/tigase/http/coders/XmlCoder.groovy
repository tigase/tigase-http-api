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
package tigase.http.coders

import groovy.xml.MarkupBuilder

import java.util.logging.Level
import java.util.logging.Logger

public class XmlCoder
		implements Coder {

	private static final Logger log = Logger.getLogger(XmlCoder.class.getCanonicalName())

	@Override
	String encode(Object obj) {
		def writer = new StringWriter();
		def xmlBuilder = new MarkupBuilder(writer);
		encodeObject(xmlBuilder, obj);
		return writer.toString()
	}

	@Override
	public Object decode(String str) {
		def node = new XmlSlurper().parseText(str);

		return convertNodeToObject(node, true);
	}

	private Object convertNodeToObject(def node, def first) {
		def children = node.children();
		if (children != null && !children.isEmpty()) {
			def isList = true;
			children.each { isList = isList && it.name() == 'item' }
			if (isList) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("converting list")
				};
				def list = [ ];
				children.each {
					list.add(convertNodeToObject(it, false))
				}
/*                def map = [:]
                map[node.name()] = list;
                return map;*/
				return list;
			} else {
				def map = [ : ];
				if (log.isLoggable(Level.FINEST)) {
					log.finest("converting map")
				};
				children.each {
					map.put(it.name(), convertNodeToObject(it, false))
				}
				if (first) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("first node = " + node.name())
					}
					def fmap = [ : ];
					fmap.put(node.name(), map);
					return fmap;
				} else {
					return map;
				}
			}
		} else {
			if (node.text() != null) {
				return node.text()
			}
		}
		return null;
	}
//    private void encodeList(MarkupBuilder builder, String name, Object obj) {
//        builder."$name" {
//            encodeObject(builder, obj);
//        }
//    }

	private void encodeObject(MarkupBuilder builder, Object obj) {
		obj.each { key, value ->
			if (value instanceof List) {
				builder."$key" {
					value.each { item ->
						if (item instanceof Map) {
							builder.item {
								encodeObject(builder, item);
							}
						} else {
							if (!(item instanceof Number || item instanceof String)) {
								item = item?.toString()
							}

							builder.item(item)
						}
					}
				}
			} else if (value instanceof Map) {
				builder."$key" {
					encodeObject(builder, value);
				}
			} else {
				if (!(value instanceof Number || value instanceof String)) {
					value = value?.toString()
				}

				builder."$key"(value)
			}
		}
	}

}
