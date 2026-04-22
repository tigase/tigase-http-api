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

import java.util.Arrays;

/**
 * Represents an accepted media type parsed from HTTP Accept header with its quality preference value.
 * <p>
 * This class parses Accept header values in the format "mime/type;q=0.8" where the q parameter
 * indicates the preference weight (0.0 to 1.0). If no q parameter is specified, a default
 * preference of 1.0 is used.
 * </p>
 */

public class AcceptedType {
	private final String mimeType;
	private final double preference;

	/**
	 * Constructs an AcceptedType by parsing an Accept header value.
	 * <p>
	 * Parses the input string to extract the MIME type and optional quality preference value.
	 * The input format is expected to be "mime/type" or "mime/type;q=value" where value is
	 * a number between 0.0 and 1.0.
	 * </p>
	 *
	 * @param in the Accept header value to parse (e.g., "text/html;q=0.9")
	 */
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

	/**
	 * Returns the MIME type extracted from the Accept header value.
	 *
	 * @return the MIME type string (e.g., "text/html", "application/json")
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Returns the quality preference value for this media type.
	 * <p>
	 * The preference value ranges from 0.0 to 1.0, where 1.0 indicates the highest preference.
	 * If no q parameter was specified in the Accept header, this returns 1.0 by default.
	 * </p>
	 *
	 * @return the quality preference value (0.0 to 1.0)
	 */
	public double getPreference() {
		return preference;
	}

	/**
	 * Determines whether this accepted MIME type is compatible with the given produced type.
	 *
	 * <p>Compatibility is evaluated as follows:
	 * <ul>
	 *   <li>If either side is {@code *}{@code /*}, it matches unconditionally — the client accepts
	 *       anything, or the server can produce anything.</li>
	 *   <li>If this accepted type has a wildcard subtype (e.g. {@code text/*}), it matches any
	 *       produced type sharing the same main type (e.g. {@code text/html}, {@code text/plain}).</li>
	 *   <li>If the produced type has a wildcard subtype (e.g. {@code image/*}), the match is
	 *       evaluated symmetrically — the server's broad capability is matched against this type's
	 *       main type.</li>
	 *   <li>Otherwise, an exact string match is required.</li>
	 * </ul>
	 *
	 * @param producedType the MIME type produced by the server (e.g. {@code application/json})
	 * @return {@code true} if this accepted type is satisfied by {@code producedType},
	 *         {@code false} otherwise
	 */
	public boolean matches(String producedType) {
		if ("*/*".equals(mimeType) || "*/*".equals(producedType)) {
			return true;
		}
		if (mimeType.endsWith("/*")) {
			String clientMain = mimeType.substring(0, mimeType.indexOf('/'));
			int slash = producedType.indexOf('/');
			return slash >= 0 && clientMain.equals(producedType.substring(0, slash));
		}
		if (producedType.endsWith("/*")) {
			String serverMain = producedType.substring(0, producedType.indexOf('/'));
			int slash = mimeType.indexOf('/');
			return slash >= 0 && serverMain.equals(mimeType.substring(0, slash));
		}
		return mimeType.equals(producedType);
	}
}