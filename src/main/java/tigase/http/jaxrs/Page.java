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

import java.util.ArrayList;
import java.util.List;

public record Page<T>(Pageable pageable, int totalCount, List<T> items) {

	public boolean isFirst() {
		return pageable.pageNumber() == 0;
	}

	public boolean isLast() {
		return (pageable.pageNumber() + 1) * pageable.pageSize() >= totalCount;
	}

	public boolean hasPrevous() {
		return !isFirst();
	}

	public boolean hasNext() {
		return !isLast();
	}

	public Pageable pageable() {
		return pageable;
	}

	public Pageable nextPageable() {
		return pageable.next();
	}

	public Pageable previousPageable() {
		return pageable.previous();
	}

	public int totalPages() {
		return (int) Math.ceil((double) totalCount / pageable.pageSize());
	}

	public List<Integer> paginate(int oddNoOfItems) {
		int part = (oddNoOfItems / 2);
		int currentPage = pageable.pageNumber();
		int start = 0;
		int end = 0;
		if (totalPages() / 2 > currentPage) {
			start = Math.max(1, currentPage + 1 - part);
			end = Math.min(totalPages(), start + oddNoOfItems - 1);
		} else {
			end = Math.min(totalPages(), currentPage + 1 + part);
			start = Math.max(1, end - oddNoOfItems + 1);
		}

		List<Integer> pages = new ArrayList<>();
		for (int i = start; i <= end; i++) {
			pages.add(i);
		}
		return pages;
	}
}
