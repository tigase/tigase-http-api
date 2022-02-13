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
package tigase.http.api.rest;

import tigase.http.api.HttpException;
import tigase.http.api.NotFoundException;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public abstract non-sealed class RestHandlerAsync<Content extends tigase.http.api.rest.contents.Content> implements RestHandler<Content> {

	private final Pattern pathPattern;

	protected RestHandlerAsync(String path) {
		this.pathPattern = Pattern.compile(path);
	}

	@Override
	public Pattern getPathPattern() {
		return pathPattern;
	}

	public CompletableFuture<Content> doGet(Context context) throws HttpException {
		throw new NotFoundException();
	}

	public CompletableFuture<Content> doPost(Context context, Content data) throws HttpException {
		throw new NotFoundException();
	}

	public CompletableFuture<Content> doDelete(Context context) throws HttpException {
		throw new NotFoundException();
	}

}