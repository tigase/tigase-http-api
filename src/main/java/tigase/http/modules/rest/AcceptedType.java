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
package tigase.http.modules.rest;

import java.util.Arrays;

public class AcceptedType {
	private final String mimeType;
	private final double preference;

	public AcceptedType(String in) {
		String[] parts = in.split(";");
		mimeType = parts[0].trim();
		preference = Arrays.stream(parts)
				.skip(1)
				.map(String::trim)
				.filter(str -> str.startsWith("q="))
				.map(str -> str.substring(2).trim())
				.map(Double::parseDouble)
				.findFirst()
				.orElse(1.0);
	}

	public String getMimeType() {
		return mimeType;
	}

	public double getPreference() {
		return preference;
	}
}