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
package tigase.http.modules.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.UnmarshalException;
import tigase.http.api.HttpException;
import tigase.http.api.rest2.RestHandler;
import tigase.http.api.rest2.marshallers.*;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RequestHandler {

	private static final Map<Class, Function<String,Object>> DESERIALIZERS = new HashMap<>();
	static {
		DESERIALIZERS.put(Long.class, Long::parseLong);
		DESERIALIZERS.put(Integer.class, Integer::parseInt);
		DESERIALIZERS.put(Double.class, Double::parseDouble);
		DESERIALIZERS.put(Float.class, Float::parseFloat);
		DESERIALIZERS.put(String.class, s -> s);
		DESERIALIZERS.put(BareJID.class, BareJID::bareJIDInstanceNS);
		DESERIALIZERS.put(JID.class, JID::jidInstanceNS);
	}

	private final RestHandler restHandler;
	private final Method method;
	private final Pattern pattern;
	private final HttpMethod httpMethod;
	private final Set<String> supportedContentTypes;

	public RequestHandler(RestHandler restHandler, Method method, HttpMethod httpMethod, Pattern pattern) {
		this.httpMethod = httpMethod;
		this.restHandler = restHandler;
		this.method = method;
		this.pattern = pattern;
		Consumes consumes = method.getAnnotation(Consumes.class);
		if (consumes != null) {
			supportedContentTypes = Arrays.stream(consumes.value()).collect(Collectors.toSet());
		} else {
			supportedContentTypes = Collections.emptySet();
		}
	}

	public RestHandler getHandler() {
		return restHandler;
	}

	public Matcher test(HttpServletRequest request, String requestUri) {
		if (supportedContentTypes.contains(request.getContentType()) || supportedContentTypes.isEmpty()) {
			return pattern.matcher(requestUri);
		}
		return null;
	}

	public void execute(HttpServletRequest request, HttpServletResponse response, Matcher matcher, ScheduledExecutorService executorService)
			throws HttpException, IOException {
		Optional<String> acceptedType = selectResponseMimeType(method, request);

		List values = new ArrayList<>();
		AsyncResponseImpl asyncResponse = null;
		try {
			for (Parameter param : method.getParameters()) {
				PathParam pathParam = param.getAnnotation(PathParam.class);
				if (pathParam != null) {
					String valueStr = matcher.group(pathParam.value());
					Object value = null;
					if (valueStr != null) {
						value = convertToValue(param.getType(), valueStr);
					}
					values.add(value);
					continue;
				}

				HeaderParam headerParam = param.getAnnotation(HeaderParam.class);
				if (headerParam != null) {
					String valueStr = request.getHeader(headerParam.value());
					Object value = null;
					if (valueStr != null) {
						value = convertToValue(param.getType(), valueStr);
					}
					values.add(value);
					continue;
				}

				if (param.getAnnotation(Suspended.class) != null) {
					if (asyncResponse == null) {
						asyncResponse = new AsyncResponseImpl(this, executorService, request, acceptedType);
					}
					values.add(asyncResponse);
					continue;
				}

				String contentType = request.getContentType();
				if (contentType != null) {
					values.add(decodeContent(param.getType(), request));
				} else {
					values.add(null);
				}
			}

			try {
				Object result = method.invoke(restHandler, values.toArray());
				if (Void.TYPE.equals(method.getReturnType())) {
					return;
				} else {
					if (result != null) {
						sendEncodedContent(result, acceptedType, response);
					} else {
						response.setStatus(200);
					}
				}
			} catch (InvocationTargetException | IllegalAccessException ex) {
				throw new HttpException(ex, 500);
			}
		} catch (Throwable ex) {
			if (asyncResponse != null) {
				asyncResponse.resume(ex);
			}
			throw ex;
		}
	}
	
	private Object convertToValue(Class expectedClass, String valueStr) {
		Function<String, Object> mapper = DESERIALIZERS.get(expectedClass);
		if (mapper == null) {
			try {
				Method method = expectedClass.getDeclaredMethod("fromString", String.class);
				return method.invoke(null, valueStr);
			} catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
				// nothing to do..
			}
			return null;
		}
		return mapper.apply(valueStr);
	}

	private Object decodeContent(Class clazz, HttpServletRequest request) throws HttpException, IOException {
		Unmarshaller unmarshaller = newUnmarshaller(request.getContentType());
		try (InputStream inputStream = request.getInputStream()) {
			return unmarshaller.unmarshal(clazz, inputStream);
		} catch (UnmarshalException e) {
			throw new HttpException(e, 422);
		}
	}

	public void sendEncodedContent(Object object, Optional<String> acceptedType, HttpServletResponse response) throws HttpException, IOException {
		try {
			if (object instanceof Response) {
				Response resp = (Response) object;

				for (Map.Entry<String,List<String>> entry : resp.getStringHeaders().entrySet()) {
					for (String it : entry.getValue()) {
						response.addHeader(entry.getKey(), it);
					}
				}

				Object entity = resp.getEntity();
				if (entity != null && entity instanceof byte[]) {
					response.setContentLength(((byte[]) entity).length);
				}

				Response.StatusType status = resp.getStatusInfo();
				response.setStatus(status.getStatusCode());

				if (entity != null) {
					if (entity instanceof byte[]) {
						response.getOutputStream().write((byte[]) entity);
					} else if (entity instanceof String) {
						response.getWriter().write((String) entity);
					} else {
						encodeObject(object, acceptedType.orElse(MediaType.APPLICATION_XML), response);
					}
				} else {
					if (status.getReasonPhrase() != null) {
						response.getWriter().write(status.getReasonPhrase());
					}
				}
			} else {
				encodeObject(object, acceptedType.orElse(MediaType.APPLICATION_XML), response);
			}
		} catch (MarshalException e) {
			throw new HttpException(e, 500);
		}
	}

	private void encodeObject(Object object, String mimeType, HttpServletResponse response)
			throws UnsupportedFormatException, MarshalException, IOException {
		Marshaller marshaller = newMarshaller(mimeType);
		response.setContentType(mimeType);
		try (OutputStream outputStream = response.getOutputStream()) {
			marshaller.marshall(object, outputStream);
		}
	}

	private Marshaller newMarshaller(String acceptedType) throws UnsupportedFormatException {
		return switch (acceptedType) {
			case "application/json" -> new JsonMarshaller();
			case "application/xml" -> new XmlMarshaller();
			default -> throw new UnsupportedFormatException("Format '" + acceptedType + "' is not supported!");
		};
	}

	protected Optional<String> selectResponseMimeType(Method method, HttpServletRequest request) throws HttpException {
		Produces produces = method.getAnnotation(Produces.class);
		if (produces == null) {
			return Optional.empty();
		}

		Set<String> supported = Arrays.stream(produces.value()).collect(Collectors.toSet());
		String header = request.getHeader("Accept");
		if (header == null) {
			return Optional.empty();
		}
		
		return Arrays.stream(header.split(",")).map(
				AcceptedType::new).filter(it -> supported.contains(it.getMimeType())).sorted(Comparator.comparing(
				AcceptedType::getPreference).reversed()).findFirst().map(
				AcceptedType::getMimeType);
	}


	private Unmarshaller newUnmarshaller(String contentType) throws UnsupportedFormatException {
		return switch (contentType) {
			case "application/json" -> new JsonUnmarshaller();
			case "application/xml" -> new XmlUnmarshaller();
			default -> throw new UnsupportedFormatException("Format '" + contentType + "' is not supported!");
		};
	}
}
