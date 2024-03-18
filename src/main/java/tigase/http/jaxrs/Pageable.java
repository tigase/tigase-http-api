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

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public record Pageable(int pageNumber, int pageSize, Sort sort) {

	public static Pageable from(HttpServletRequest request) {
		int page = Optional.ofNullable(request.getParameter("page")).map(Integer::parseInt).orElse(0);
		int size = Optional.ofNullable(request.getParameter("size")).map(Integer::parseInt).orElse(30);
		Sort sort = Optional.ofNullable(request.getParameter("sort")).map(Sort::valueOf).orElse(Sort.asc);
		return new Pageable(page, size, sort);
	}

	public int offset() {
		return pageSize * pageNumber;
	}

	public Pageable next() {
		return new Pageable(pageNumber + 1, pageSize, sort);
	}

	public Pageable previous() {
		return new Pageable(pageNumber - 1, pageSize, sort);
	}
}
