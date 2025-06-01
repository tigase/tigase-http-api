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
package tigase.http.api;

import org.eclipse.jetty.http.BadMessageException;

public class HttpException extends BadMessageException {

	public HttpException(String message, int code) {
		super(code, message);
	}

	public HttpException(String message, int code, Throwable cause) {
		super(code, message, cause);
	}

	public HttpException(Throwable cause, int code) {
		super(code, null, cause);
	}
	
}
