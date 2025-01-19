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
package tigase.http.java.filters;

import tigase.annotations.TigaseDeprecated;
import tigase.http.AbstractHttpServer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

@TigaseDeprecated(removeIn = "9.0.0", since = "8.5.0", note = "RequestHandler will be removed")
@Deprecated
public class DummyFilterConfig implements FilterConfig {

	private static final Enumeration<String> parameters = Collections.enumeration(Arrays.asList("serverBeanName"));

	private final String name;
	private final String serverBeanName;

	public DummyFilterConfig(Class<? extends Filter> filterClass, AbstractHttpServer server) {
		name = filterClass.getName() + "-" + Integer.toHexString(this.hashCode());
		serverBeanName = server.getName();
	}

	@Override
	public String getFilterName() {
		return name;
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public String getInitParameter(String s) {
		if ("serverBeanName".equals(s)) {
			return serverBeanName;
		}
		return null;
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return parameters;
	}
}
