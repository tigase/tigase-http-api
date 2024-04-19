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
package tigase.http.util;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;

import java.net.URI;
import java.nio.file.Paths;

public class TemplateUtils {

	public static TemplateEngine create(String templatesPath, String packageName, ContentType contentType) {
		if (templatesPath == null || templatesPath.isBlank()) {
			return TemplateEngine.createPrecompiled(null, contentType, null, packageName);
		} else {
			return TemplateEngine.create(new DirectoryCodeResolver(Paths.get(URI.create(
					"file:///" + templatesPath))), contentType);
		}
	}

}
