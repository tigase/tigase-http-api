/*
 * CSSHelper.java
 *
 * Tigase HTTP API
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 11.06.2017.
 */
public class CSSHelper {
	
	public static String getCssFileContent(String pathStr) throws IOException {
		pathStr = "tigase/assets/css/" + pathStr;
		Path path = Paths.get(pathStr);
		if (Files.exists(path)) {
			return Files.readAllLines(path, StandardCharsets.UTF_8).stream().collect(Collectors.joining("\n"));
		} else {
			try (InputStream is = CSSHelper.class.getResourceAsStream("/"+pathStr)) {
				if (is == null) {
					return null;
				}
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
					StringBuilder sb = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
						sb.append("\n");
					}
					return sb.toString();
				}
			}
		}
	}
}
