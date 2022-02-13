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

import tigase.http.json.JsonParser;
import tigase.http.api.HttpException;
import tigase.http.api.rest.ContentDecoder;
import tigase.xml.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ContentObjectDecoder implements ContentDecoder<ContentObject> {

	private static final SimpleParser xmlParser = SingletonFactory.getParserInstance();

	@Override
	public ContentObject decode(HttpServletRequest request) throws IOException, HttpException {
		String mimeType = Optional.ofNullable(request.getContentType()).map(str -> str.split(";")[0]).orElse("");
		return switch (mimeType) {
			case "application/json" -> decodeJson(request);
			case "application/xml", "text/xml" -> decodeXml(request);
			default -> throw new HttpException("Invalid content type!", 400);
		};
	}

	protected ContentObject decodeJson(HttpServletRequest request) throws IOException, HttpException {
		try {
			Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readContent(request));
			return new ContentObject(result);
		} catch (JsonParser.InvalidJsonException ex) {
			throw new HttpException("Malformed request syntax", 400, ex);
		}
	}

	protected ContentObject decodeXml(HttpServletRequest request) throws IOException, HttpException {
		DomBuilderHandler handler = new DomBuilderHandler();
		xmlParser.parse(handler, readContent(request));
		Element rootEl = handler.getParsedElements().poll();
		if (rootEl == null) {
			throw new HttpException("Malformed request syntax", 400);
		}

		return new ContentObject((Map<String, Object>) xmlElementToObject(rootEl));
	}

	protected Object xmlElementToObject(Element node) {
		List<Element> children = node.getChildren();
		if (children != null && !children.isEmpty()) {
			boolean isList = children.stream().allMatch(it -> it.getName() == "item");
			if (isList) {
				return children.stream().map(it -> xmlElementToObject(it)).collect(Collectors.toList());
			} else {
				Map<String, Object> object = new HashMap<>();
				for (Element child : children) {
					object.put(child.getName(), xmlElementToObject(child));
				}
				return object;
			}
		} else {
			return XMLUtils.unescape(node.getCData());
		}
	}

	protected String readContent(HttpServletRequest request) throws IOException {
		return request.getReader().lines().collect(Collectors.joining("\n"));
	}
}
