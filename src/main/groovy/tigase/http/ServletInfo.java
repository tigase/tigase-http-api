/**
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
package tigase.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServlet;

/**
 *
 * @author andrzej
 */
public class ServletInfo {

	private final String name;
	private final Class<? extends HttpServlet> servletClass;
	private final Map<String,String> initParams = new HashMap<String,String>();
	private final List<String> mappings = new ArrayList<String>();
	
	protected ServletInfo(String name, Class<? extends HttpServlet> servletClass) {
		this.name = name;
		this.servletClass = servletClass;
	}
	
	public String getName() {
		return name;
	}
	
	public Class<? extends HttpServlet> getServletClass() {
		return servletClass;
	}
	
	public ServletInfo addInitParam(String key, String value) {
		initParams.put(key,value);
		return this;
	}
	
	public Map<String,String> getInitParams() {
		return Collections.unmodifiableMap(initParams);
	}
	
	public ServletInfo addMapping(String mapping) {
		mappings.add(mapping);
		return this;
	}
	
	public List<String> getMappings() {
		return Collections.unmodifiableList(mappings);
	}
	
}
