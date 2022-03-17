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
package tigase.http.modules.rest.docs;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Path;
import tigase.http.jaxrs.HttpMethod;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class Endpoint {

	private static final List<Character> TO_REPLACE_IN_ID = List.of('{', '}', '(', ')', '?', '/');

	public static Endpoint create(Method method) {
		HttpMethod httpMethod = HttpMethod.valueOf(method);
		Optional<Operation> operation = Optional.ofNullable(method.getAnnotation(Operation.class));

		String fullPath = Optional.ofNullable(method.getDeclaringClass().getAnnotation(
				Path.class)).map(Path::value).orElse("");
		Path pathAnnotation = method.getAnnotation(Path.class);
		if (pathAnnotation != null) {
			if (!pathAnnotation.value().startsWith("/")) {
				fullPath = fullPath + "/";
			}

			fullPath = fullPath + pathAnnotation.value();
		}

		String id = fullPath;
		for (Character ch : TO_REPLACE_IN_ID) {
			id = id.replace(ch, '-');
		}

		List<tigase.http.modules.rest.docs.Parameter> parameters = Parameter.create(method);

		return new Endpoint(id + method.getName(), httpMethod, fullPath, operation.map(Operation::summary).filter(s -> !s.isEmpty()).orElse(null),
							operation.map(Operation::description).filter(s -> !s.isEmpty()).orElse(null), parameters, Response.create(method));
	}

	private final String id;
	private final HttpMethod method;
	private final List<Parameter> parameters;
	private final String path;
	private final String name;
	private final String description;
	private final List<Response> responses;

	public Endpoint(String id, HttpMethod method, String path, String name, String description,
					List<Parameter> parameters, List<Response> responses) {
		this.id = id;
		this.method = method;
		this.path = path;
		this.name = name;

		this.description = description;
		this.parameters = parameters;
		this.responses = responses;
	}

	public String getId() {
		return id;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getPath() {
		return path;
	}

	public List<Response> getResponses() {
		return responses;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

}
