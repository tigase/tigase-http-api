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

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UriBuilderImpl extends UriBuilder {

	private String scheme;
	private String userInfo;
	private String host;
	private String port;
	private final MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl<String>();
	private String fragment;
	private List<String> path = new ArrayList<>();

	public UriBuilderImpl() {}

	@Override
	public UriBuilder clone() {
		UriBuilderImpl builder = new UriBuilderImpl();
		builder.scheme = scheme;
		builder.host = host;
		builder.port = port;
		for (Map.Entry<String,List<String>> e : queryParams.entrySet()) {
			builder.queryParams.addAll(e.getKey(), e.getValue());
		}
		builder.fragment = fragment;
		builder.path = new ArrayList<>(path);
		return builder;
	}

	@Override
	public UriBuilder uri(URI uri) {
		scheme = uri.getScheme();
		host = uri.getHost();
		if (uri.getPort() > 0) {
			port = String.valueOf(uri.getPort());
		} else {
			port = null;
		}
		path.clear();
		Optional.ofNullable(uri.getPath())
				.stream()
				.flatMap(path -> Arrays.stream(path.split("/")))
				.filter(Predicate.not(String::isBlank))
				.forEach(path::add);
		queryParams.clear();
		Optional.ofNullable(uri.getQuery())
				.stream()
				.flatMap(q -> Arrays.stream(q.split("&")))
				.map(p -> p.split("="))
				.forEach(pair -> {
					String key = pair[0];
					String value = pair.length > 1 ? pair[1] : null;
					queryParams.add(key, value);
				});
		fragment = uri.getFragment();
		return this;
	}

	@Override
	public UriBuilder uri(String s) {
		return null;
	}

	@Override
	public UriBuilder scheme(String s) {
		scheme = s;
		return this;
	}

	@Override
	public UriBuilder schemeSpecificPart(String s) {
		return null;
	}

	@Override
	public UriBuilder userInfo(String s) {
		return null;
	}

	@Override
	public UriBuilder host(String s) {
		host = s;
		return this;
	}

	@Override
	public UriBuilder port(int i) {
		if (i > 0) {
			port = String.valueOf(i);
		} else {
			port = null;
		}
		return this;
	}

	@Override
	public UriBuilder replacePath(String s) {
		path.clear();
		return path(s);
	}

	@Override
	public UriBuilder path(String s) {
		if (s != null) {
			Arrays.stream(s.split("/")).filter(Predicate.not(String::isBlank)).forEach(path::add);
		}
		return this;
	}

	@Override
	public UriBuilder path(Class aClass) {
		Path path = (Path) aClass.getAnnotation(Path.class);
		if (path == null) {
			throw new RuntimeException("Class " + aClass.getCanonicalName() + " is not annotated with @Path!");
		}
		return path(path.value());
	}

	@Override
	public UriBuilder path(Class aClass, String methodName) {
		List<Method> methods = Arrays.stream(aClass.getMethods())
				.filter(method -> methodName.equals(method.getName()))
				.toList();
		if (methods.isEmpty()) {
			throw new RuntimeException("Class " + aClass.getCanonicalName() + " doesn't have method " + methodName + "!");
		} else if (methods.size() > 1) {
			throw new RuntimeException("Class " + aClass.getCanonicalName() + " has multiple methods named " + methodName + "!");
		}
		return path(methods.get(0));
	}

	@Override
	public UriBuilder path(Method method) {
		Path classPath = method.getDeclaringClass().getAnnotation(Path.class);
		Path methodPath = method.getAnnotation(Path.class);
		if (methodPath == null) {
			throw new RuntimeException("Method " + method + " is not annotated with @Path!");
		}
		return path(classPath != null ? classPath.value() : "").path(methodPath.value());
	}

	@Override
	public UriBuilder segment(String... strings) {
		return null;
	}

	@Override
	public UriBuilder replaceMatrix(String s) {
		return null;
	}

	@Override
	public UriBuilder matrixParam(String s, Object... objects) {
		return null;
	}

	@Override
	public UriBuilder replaceMatrixParam(String s, Object... objects) {
		return null;
	}

	@Override
	public UriBuilder replaceQuery(String s) {
		queryParams.clear();
		return this;
	}

	@Override
	public UriBuilder queryParam(String s, Object... objects) {
		for (Object o : objects) {
			if (o == null) {
				continue;
			}
			queryParams.addAll(s, o.toString());
		}
		return this;
	}

	@Override
	public UriBuilder replaceQueryParam(String s, Object... objects) {
		queryParams.putSingle(s,null);
		queryParam(s, objects);
		return this;
	}

	@Override
	public UriBuilder fragment(String s) {
		fragment = s;
		return this;
	}

	@Override
	public UriBuilder resolveTemplate(String s, Object o) {
		return null;
	}

	@Override
	public UriBuilder resolveTemplate(String s, Object o, boolean b) {
		return null;
	}

	@Override
	public UriBuilder resolveTemplateFromEncoded(String s, Object o) {
		return null;
	}

	@Override
	public UriBuilder resolveTemplates(Map<String, Object> map) {
		return null;
	}

	@Override
	public UriBuilder resolveTemplates(Map<String, Object> map, boolean b) throws IllegalArgumentException {
		return null;
	}

	@Override
	public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> map) {
		return null;
	}

	@Override
	public URI buildFromMap(Map<String, ?> map) {
		return null;
	}

	@Override
	public URI buildFromMap(Map<String, ?> map, boolean b) throws IllegalArgumentException, UriBuilderException {
		return null;
	}

	@Override
	public URI buildFromEncodedMap(Map<String, ?> map) throws IllegalArgumentException, UriBuilderException {
		return null;
	}

	@Override
	public URI build(Object... objects) throws IllegalArgumentException, UriBuilderException {
		StringBuilder sb = new StringBuilder();
		if (scheme != null) {
			sb.append(scheme).append("://");
		}
		if (host != null) {
			sb.append(host);
			if (port != null) {
				if (scheme == null || ("https".equals(scheme) && !"443".equals(port)) ||
						("http".equals(scheme) && !port.equals("80")) || (!scheme.startsWith("http"))) {
					sb.append(":").append(port);
				}
			}
		}
		sb.append("/");
		AtomicInteger paramCounter = new AtomicInteger();
		if (path != null) {
			sb.append(path.stream().map(part -> {
				if (part.startsWith("{") && part.endsWith("}")) {
					int pos = paramCounter.getAndIncrement();
					if (pos > objects.length - 1) {
						throw new IllegalArgumentException("Missing value for path parameter " + part);
					}
					return objects[pos].toString();
				} else {
					return part;
				}
			}).collect(Collectors.joining("/")));
			//sb.append(String.join("/", path));
		}
		if (!queryParams.isEmpty()) {
			sb.append("?");
			for (Map.Entry<String,List<String>> e : queryParams.entrySet()) {
				if (e.getValue() != null) {
					for (String value : e.getValue()) {
						sb.append(e.getKey()).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append("&");
					}
				}
			}
		}
		if (fragment != null) {
			sb.append("#").append(fragment);
		}
		return URI.create(sb.toString());
	}

	@Override
	public URI build(Object[] objects, boolean b) throws IllegalArgumentException, UriBuilderException {
		return null;
	}

	@Override
	public URI buildFromEncoded(Object... objects) throws IllegalArgumentException, UriBuilderException {
		return null;
	}

	@Override
	public String toTemplate() {
		return null;
	}
}
