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

import tigase.http.api.HttpException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface RequestHandler extends Comparable<RequestHandler> {

	public static Comparator<Pattern> PATTERN_COMPARATOR = Comparator.comparing((Pattern pattern) -> pattern.pattern().length()).reversed();
	
	Handler getHandler();

	HttpMethod getHttpMethod();

	Handler.Role getRequiredRole();

	Matcher test(HttpServletRequest request, String requestUri);

	void execute(HttpServletRequest request, HttpServletResponse response, Matcher matcher,
						ScheduledExecutorService executorService) throws HttpException, IOException;
}
