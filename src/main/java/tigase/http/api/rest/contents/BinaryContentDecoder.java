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

import tigase.http.api.rest.ContentDecoder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class BinaryContentDecoder implements ContentDecoder<BinaryContent> {

	@Override
	public BinaryContent decode(HttpServletRequest request) throws IOException {
		String mimeType = request.getContentType();
		byte[] data = request.getInputStream().readAllBytes();
		return new BinaryContent(mimeType, data);
	}
}
