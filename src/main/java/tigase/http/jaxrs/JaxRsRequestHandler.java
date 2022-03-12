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

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.UnmarshalException;
import tigase.http.api.HttpException;
import tigase.http.api.UnsupportedFormatException;
import tigase.http.jaxrs.marshallers.*;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JaxRsRequestHandler
		implements RequestHandler {

	private static final Logger log = Logger.getLogger(JaxRsRequestHandler.class.getCanonicalName());
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

	private final Handler.Role requiredRole;
	private final Handler handler;
	private final Method method;
	private final Pattern pattern;
	private final HttpMethod httpMethod;
	private final Set<String> supportedContentTypes;

	public static List<JaxRsRequestHandler> create(Handler instance) {
		Path path = instance.getClass().getAnnotation(Path.class);
		if (path == null) {
			return Collections.emptyList();
		}

		ArrayList<JaxRsRequestHandler> handlers = new ArrayList<>();
		Method[] methods = instance.getClass().getDeclaredMethods();
		for (Method method : methods) {
			JaxRsRequestHandler handler = JaxRsRequestHandler.create(path.value(), instance, method);
			if (handler != null) {
				handlers.add(handler);
			}
		}
		return handlers;
	}

	public static JaxRsRequestHandler create(String contextPath, Handler instance, Method method) {
		if (!Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		HttpMethod httpMethod = getHttpMethod(method);
		if (httpMethod == null) {
			return null;
		}
		
		String methodPath = Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse("");

		String fullPath = contextPath;
		if (!methodPath.isEmpty() && !methodPath.startsWith("/")) {
			fullPath = fullPath + "/";
		}

		fullPath = fullPath + methodPath;

		Pattern pattern = prepareMatcher(fullPath, method);
		return new JaxRsRequestHandler(instance, method, httpMethod, pattern, instance.getRequiredRole());
	}

	public static HttpMethod getHttpMethod(Method method) {
		if (method.getAnnotation(GET.class) != null) {
			return HttpMethod.GET;
		}
		if (method.getAnnotation(POST.class) != null) {
			return HttpMethod.POST;
		}
		if (method.getAnnotation(PUT.class) != null) {
			return HttpMethod.PUT;
		}
		if (method.getAnnotation(DELETE.class) != null) {
			return HttpMethod.DELETE;
		}
		return null;
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	public Handler.Role getRequiredRole() {
		return requiredRole;
	}

	public static Pattern prepareMatcher(String path, Method method) {
		Map<String, Class> paramClasses = methodsPathParams(method);

		int idx = -1;

		List<Param> params = new ArrayList<>();

		while ((idx = path.indexOf('{', idx + 1)) > -1) {
			int startIdx = idx;
			int endIdx = idx;
			while ((endIdx = path.indexOf('}', endIdx) ) > -1 && path.charAt(endIdx) == '\\') {
			}
			if (endIdx == -1) {
				// this will not work
				return null;
			}

			String paramName = path.substring(idx+1, endIdx).trim();
			String paramRegex = regexForClass(paramClasses.get(paramName));

			if (paramRegex == null) {
				return null;
			}

			params.add(new Param(paramName, paramRegex, startIdx, endIdx+1));
		}

		String regex = path;

		for (int i=params.size() - 1; i >= 0; i--) {
			Param param = params.get(i);
			String prefix = regex.substring(0, param.startIdx);
			String suffix = regex.substring(param.endIdx);
			regex = prefix + "(?<" + param.name + ">" + param.regex + ")" + suffix;
		}

		return Pattern.compile(regex);
	}

	private static class Param {
		private final String name;
		private final String regex;
		private final int startIdx;
		private final int endIdx;

		private Param(String name, String regex, int startIdx, int endIdx) {
			this.name = name;
			this.regex = regex;
			this.startIdx = startIdx;
			this.endIdx = endIdx;
		}
	}

	private static String regexForClass(Class clazz) {
		if (Long.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
			return "[0-9]+";
		}
		if (String.class.isAssignableFrom(clazz)) {
			return "[^\\/]+";
		}
		if (BareJID.class.isAssignableFrom(clazz)) {
			return "[^\\/]+";
		}
		try {
			Method m = clazz.getDeclaredMethod("fromString", String.class);
			if (Modifier.isStatic(m.getModifiers())) {
				return "[^\\/]+";
			}
		} catch (NoSuchMethodException e) {
			log.log(Level.FINEST, "Method 'fromString' for conversation to object from String not found", e);
			throw new RuntimeException(e);
			// nothing to do..
		}
		return null;
	}

	private static Map<String,Class> methodsPathParams(Method method) {
		Map<String, Class> params = new HashMap<>();
		for (Parameter parameter : method.getParameters()) {
			PathParam pathParam = parameter.getAnnotation(PathParam.class);
			if (pathParam == null) {
				continue;
			}
			params.put(pathParam.value(), parameter.getType());
		}
		return params;
	}

	public JaxRsRequestHandler(Handler handler, Method method, HttpMethod httpMethod, Pattern pattern, Handler.Role requiredRole) {
		this.requiredRole = requiredRole;
		this.httpMethod = httpMethod;
		this.handler = handler;
		this.method = method;
		this.pattern = pattern;
		Consumes consumes = method.getAnnotation(Consumes.class);
		if (consumes != null) {
			supportedContentTypes = Arrays.stream(consumes.value()).collect(Collectors.toSet());
		} else {
			supportedContentTypes = Collections.emptySet();
		}
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
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
				Object value = null;
				PathParam pathParam = param.getAnnotation(PathParam.class);
				HeaderParam headerParam = param.getAnnotation(HeaderParam.class);
				FormParam formParam = param.getAnnotation(FormParam.class);
				
				if (pathParam != null) {
					String valueStr = matcher.group(pathParam.value());
					if (valueStr == null) {
						valueStr = getParamDefaultValue(param);
					}
					if (valueStr != null) {
						value = convertToValue(param.getType(), valueStr);
					}
				} else if (headerParam != null) {
					String valueStr = request.getHeader(headerParam.value());
					if (valueStr == null) {
						valueStr = getParamDefaultValue(param);
					}
					if (valueStr != null) {
						value = convertToValue(param.getType(), valueStr);
					}
				} else if (formParam != null) {
					String[] valuesStr = request.getParameterValues(formParam.value());
					if (valuesStr == null) {
						valuesStr = new String[]{getParamDefaultValue(param)};
					}
					if (boolean.class.equals(param.getType())) {
						value = valuesStr != null && valuesStr.length == 1 && "on".equals(valuesStr[0]);
					} else {
						if (valuesStr != null) {
							value = convertToValue(param.getParameterizedType(), valuesStr);
						}
					}
				} else if (param.getAnnotation(Suspended.class) != null) {
					if (asyncResponse == null) {
						asyncResponse = new AsyncResponseImpl(this, executorService, request, acceptedType);
					}
					value = asyncResponse;
				} else if (param.getAnnotation(BeanParam.class) != null) {
					try {
						value = new WWWFormUrlEncodedUnmarshaller().unmarshal(param.getType(), request);
					} catch (UnmarshalException ex) {
						throw new HttpException(ex, HttpServletResponse.SC_NOT_ACCEPTABLE);
					}
				} else if (SecurityContext.class.isAssignableFrom(param.getType())) {
					value = new SecurityContextImpl(request);
				} else if (HttpServletRequest.class.isAssignableFrom(param.getType())) {
					value = request;
				} else {
					// if non on the above..
					String contentType = request.getContentType();
					if (contentType != null) {
						value = decodeContent(param.getType(), request);
						if (value != null) {
							validateContent(value);
						}
					}
				}

				boolean notNull = param.getAnnotation(NotNull.class) != null;
				if (notNull && value == null) {
					throw new ValidationException("Parameter " + param.getName() + " cannot be NULL!");
				}
				values.add(value);
			}

			try {
				Object result = method.invoke(handler, values.toArray());
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
				if (ex.getCause() instanceof HttpException) {
					throw (HttpException) ex.getCause();
				}
				throw new HttpException(ex, 500);
			}
		} catch (Throwable ex) {
			if (asyncResponse != null) {
				asyncResponse.resume(ex);
			}
			throw ex;
		}
	}

	private void validateContent(Object object) throws HttpException {
		try {
			for (Field field : object.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				if (field.getAnnotation(NotNull.class) != null) {
					if (field.get(object) == null) {
						throw new ValidationException(
								"Field " + field.getName() + " in object " + object.getClass() + " cannot be null!");
					}
				}
			}
		} catch (IllegalAccessException ex) {
			throw new HttpException(ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private String getParamDefaultValue(Parameter param) {
		DefaultValue defValue = param.getAnnotation(DefaultValue.class);
		if (defValue == null) {
			return null;
		}
		return defValue.value();
	}

	public static Object convertToValue(Type expectedType, String[] valueStrs) {
		if (expectedType instanceof ParameterizedType) {
			Type[] params = ((ParameterizedType) expectedType).getActualTypeArguments();
			if (params == null || params.length != 1 || (!(params[0] instanceof Class<?>))) {
				return null;
			}
			return convertToValue((Class<?>) ((ParameterizedType) expectedType).getRawType(), (Class<?>) params[0], valueStrs);
		} else {
			return convertToValue((Class<?>) expectedType, null, valueStrs);
		}
	}

	public static Object convertToValue(Class<?> expectedClass, Class<?> parameterClass, String[] valueStrs) {
		if (Collection.class.isAssignableFrom(expectedClass)) {
			if (parameterClass == null) {
				return null;
			}
			Stream<Object> stream = Arrays.stream(valueStrs).map(str -> convertToValue(parameterClass, str));
			if (List.class.isAssignableFrom(expectedClass)) {
				return stream.collect(Collectors.toUnmodifiableList());
			} else if (Set.class.isAssignableFrom(expectedClass)) {
				return stream.collect(Collectors.toUnmodifiableSet());
			} else if (SortedSet.class.isAssignableFrom(expectedClass)) {
				return Collections.unmodifiableSortedSet(new TreeSet<>(stream.collect(Collectors.toList())));
			} else {
				return null;
			}
		}
		if (valueStrs.length != 1) {
			return null;
		}
		return convertToValue(expectedClass, valueStrs[0]);
	}

	private static Object convertToValue(Class expectedClass, String valueStr) {
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
