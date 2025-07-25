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
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.UnmarshalException;
import tigase.http.api.HttpException;
import tigase.http.api.UnsupportedFormatException;
import tigase.http.jaxrs.marshallers.*;
import tigase.http.jaxrs.validators.ConstraintViolation;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
		DESERIALIZERS.put(long.class, Long::parseLong);
		DESERIALIZERS.put(int.class, Integer::parseInt);
		DESERIALIZERS.put(Long.class, Long::parseLong);
		DESERIALIZERS.put(Integer.class, Integer::parseInt);
		DESERIALIZERS.put(Double.class, Double::parseDouble);
		DESERIALIZERS.put(Float.class, Float::parseFloat);
		DESERIALIZERS.put(String.class, s -> s);
		DESERIALIZERS.put(BareJID.class, str -> {
			try {
				return BareJID.bareJIDInstance(str);
			} catch (TigaseStringprepException ex) {
				throw new IllegalArgumentException(ex.getMessage(), ex);
			}
		});
		DESERIALIZERS.put(JID.class, str -> {
			try {
				return JID.jidInstance(str);
			} catch (TigaseStringprepException ex) {
				throw new IllegalArgumentException(ex.getMessage(), ex);
			}
		});
	}

	private final Handler.Role requiredRole;
	private final Set<String> allowedRoles;
	private final Handler handler;
	private final Method method;
	private final Pattern pattern;
	private final HttpMethod httpMethod;
	private final Set<String> consumedContentTypes;
	private final Set<String> producedContentTypes;

	public static List<JaxRsRequestHandler> create(Handler instance) {
		Path path = instance.getClass().getAnnotation(Path.class);
//		if (path == null) {
//			return Collections.emptyList();
//		}

		ArrayList<JaxRsRequestHandler> handlers = new ArrayList<>();
		Method[] methods = instance.getClass().getDeclaredMethods();
		for (Method method : methods) {
			JaxRsRequestHandler handler = JaxRsRequestHandler.create(path == null ? "" : path.value(), instance, method);
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

		Set<String> allowedRoles = Handler.getAllowedRoles(method);

		String methodPath = Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse("");

		String fullPath = contextPath;
		if (!methodPath.isEmpty() && !methodPath.startsWith("/")) {
			fullPath = fullPath + "/";
		}

		fullPath = fullPath + methodPath;

		Pattern pattern = prepareMatcher(fullPath, method);
		return new JaxRsRequestHandler(instance, method, httpMethod, pattern, instance.getRequiredRole(), allowedRoles);
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

	public Method getMethod() {
		return method;
	}

	public Pattern getPattern() {
		return pattern;
	}

	@Override
	public Set<String> getAllowedRoles() {
		return allowedRoles;
	}

	@Override
	public Handler.Role getRequiredRole() {
		return requiredRole;
	}

	@Override
	public boolean isAuthenticationRequired() {
		return RequestHandler.super.isAuthenticationRequired() || allowedRoles != null;
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
		if (Long.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz) || long.class.isAssignableFrom(clazz)) {
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
			try {
				Method m = clazz.getDeclaredMethod("valueOf", String.class);
				if (Modifier.isStatic(m.getModifiers())) {
					return "[^\\/]+";
				}
			} catch (NoSuchMethodException ex) {
				log.log(Level.FINEST, "Method 'fromString' or 'valueOf' for conversation to object from String not found", e);
				throw new RuntimeException(e);
			}
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

	public JaxRsRequestHandler(Handler handler, Method method, HttpMethod httpMethod, Pattern pattern, Handler.Role requiredRole, Set<String> allowedRoles) {
		this.requiredRole = requiredRole;
		this.httpMethod = httpMethod;
		this.handler = handler;
		this.method = method;
		this.pattern = pattern;
		this.allowedRoles = allowedRoles;
		Consumes consumes = method.getAnnotation(Consumes.class);
		if (consumes != null) {
			consumedContentTypes = Arrays.stream(consumes.value()).collect(Collectors.toSet());
		} else {
			consumedContentTypes = Collections.emptySet();
		}
		Produces produces = method.getAnnotation(Produces.class);
		if (produces != null) {
			producedContentTypes = Arrays.stream(produces.value()).collect(Collectors.toSet());
		} else {
			producedContentTypes = Collections.emptySet();
		}
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}
	
	public Matcher test(HttpServletRequest request, String requestUri) {
		if (consumedContentTypes.contains(request.getContentType()) || consumedContentTypes.isEmpty()) {
			String header = request.getHeader("Accept");
			if (header != null && !producedContentTypes.isEmpty()) {
				if (Arrays.stream(header.split(",")).map(
						AcceptedType::new).filter(it -> producedContentTypes.contains(it.getMimeType())).sorted(Comparator.comparing(
						AcceptedType::getPreference).reversed()).findFirst().map(
						AcceptedType::getMimeType).isEmpty()) {
					return null;
				}
			}
			return pattern.matcher(requestUri);
		}
		return null;
	}

	private String getParameterNameDescription(Parameter param) {
		PathParam pathParam = param.getAnnotation(PathParam.class);
		HeaderParam headerParam = param.getAnnotation(HeaderParam.class);
		FormParam formParam = param.getAnnotation(FormParam.class);
		QueryParam queryParam = param.getAnnotation(QueryParam.class);

		if (pathParam != null) {
			return "Parameter '" + pathParam.value() + "'";
		} else if (headerParam != null) {
			return "Header '" + headerParam.value() + "'";
		} else if (formParam != null) {
			return "Form parameter '" + formParam.value() + "'";
		} else if (queryParam != null) {
			return "Query parameter '" + queryParam.value() + "'";
		} else {
			return "Unknown " + param.getName();
		}
	}

	public void execute(HttpServletRequest request, HttpServletResponse response, Matcher matcher, ScheduledExecutorService executorService)
			throws HttpException, IOException {
		ContainerRequestContext context = new ContainerRequestContext(request);

		Optional<String> acceptedType = selectResponseMimeType(method, request);
		List values = new ArrayList<>();
		AsyncResponseImpl asyncResponse = null;
		try {
			List<ConstraintViolation> violations = new ArrayList<>();
			Map<Parameter, ValidationException> parsingExceptions = new HashMap<>();
			for (Parameter param : method.getParameters()) {
				Object value = null;

				PathParam pathParam = param.getAnnotation(PathParam.class);
				HeaderParam headerParam = param.getAnnotation(HeaderParam.class);
				FormParam formParam = param.getAnnotation(FormParam.class);
				QueryParam queryParam = param.getAnnotation(QueryParam.class);

				try {
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
							String defValue = getParamDefaultValue(param);
							if (defValue != null) {
								valuesStr = new String[]{getParamDefaultValue(param)};
							}
						}
						if (boolean.class.equals(param.getType())) {
							value = valuesStr != null && valuesStr.length == 1 && "on".equals(valuesStr[0]);
						} else {
							if (valuesStr != null) {
								value = convertToValue(param.getParameterizedType(), valuesStr);
							}
						}
					} else if (queryParam != null) {
						String[] valuesStr = request.getParameterValues(queryParam.value());
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
					} else if (Pageable.class.isAssignableFrom(param.getType())) {
						value = Pageable.from(request);
					} else if (SecurityContext.class.isAssignableFrom(param.getType())) {
						value = context.getSecurityContext();
					} else if (HttpServletRequest.class.isAssignableFrom(param.getType())) {
						value = context.getRequest();
					} else if (HttpServletResponse.class.isAssignableFrom(param.getType())) {
						value = response;
					} else if (UriInfo.class.isAssignableFrom(param.getType())) {
						value = context.getUriInfo();
					} else if (Model.class.isAssignableFrom(param.getType())) {
						value = new Model(context);
					} else {
						// if non on the above..
						String contentType = request.getContentType();
						if (contentType != null) {
							value = decodeContent(param.getType(), request);
						}
					}
				} catch (ValidationException ex) {
					log.log(Level.FINER, ex, () -> "failed to parse request parameter");
					parsingExceptions.put(param, ex);
				}
				values.add(value);
			}

			try {
				ContainerRequestContext.setContext(context);
				validateParameters(handler, method, values.toArray(), parsingExceptions::containsKey, violations::add);

				if ((!violations.isEmpty()) || (!parsingExceptions.isEmpty())) {
					throwValidationError(violations, parsingExceptions.values());
				}

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
			} catch (InvocationTargetException|IllegalAccessException ex) {
				unwrapInvocationTargetException(ex);
			} finally {
				ContainerRequestContext.resetContext();
			}
		} catch (Throwable ex) {
			if (asyncResponse != null) {
				asyncResponse.resume(ex);
			}
			log.log(Level.WARNING, "Exception while processing request", ex);
			throw ex;
		}
	}

	private void unwrapInvocationTargetException(Throwable ex) throws HttpException {
		if (ex instanceof InvocationTargetException) {
			Throwable cause = ex.getCause();
			if (cause != null) {
				unwrapInvocationTargetException(cause);
			}
		} else {
			if (ex instanceof HttpException) {
				throw (HttpException) ex.getCause();
			}
			if (ex instanceof TigaseStringprepException) {
				throw new ValidationException(ex.getMessage(), ex);
			}
			throw new HttpException(ex, 500);
		}
	}

	private void validateParameters(Object handler, Method method, Object[] values, Predicate<Parameter> skipValidation, Consumer<ConstraintViolation> consumer) throws HttpException {
		var parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			if (skipValidation.test(parameter)) {
				continue;
			}
			Object value = values[i];
			validateValue(parameter, parameter.getType(), value, null, consumer);
		}
	}
	
	private void validateValue(AnnotatedElement store, Class<?> type, Object value, ConstraintViolation.Path path, Consumer<ConstraintViolation> consumer) throws HttpException {
		var annotations = store.getAnnotations();
		for (var annotation : annotations) {
			var validators = getValidators(annotation);
			for (var validator : validators) {
				if (!validator.test(value)) {
					consumer.accept(createViolation(path, annotation, store, value));
				}
			}

			if (annotation instanceof NotNull || type.isPrimitive()) {
				if (!Objects.nonNull(value)) {
					consumer.accept(createViolation(path, annotation, store, value));
				}
			}
			else if (annotation instanceof NotEmpty) {
				if (String.class.isAssignableFrom(type)) {
					if (value == null || value.toString().isEmpty()) {
						consumer.accept(createViolation(path, annotation, store, value));
					}
				}
				if (Collection.class.isAssignableFrom(type)) {
					if (value == null || ((Collection) value).isEmpty()) {
						consumer.accept(createViolation(path, annotation, store, value));
					}
				}
			} else if (annotation instanceof NotBlank) {
				if (String.class.isAssignableFrom(type)) {
					if (value == null || value.toString().isBlank()) {
						consumer.accept(createViolation(path, annotation, store, value));
					}
				}
			} else if (annotation instanceof javax.validation.constraints.Pattern) {
				if (value == null || !Pattern.matches(((javax.validation.constraints.Pattern) annotation).regexp(), value.toString())) {
					consumer.accept(createViolation(path, annotation, store, value));
				}
			} else if (annotation instanceof Valid || path != null) {
				if (value != null) {
					if (value instanceof Collection collection) {
						var items = new ArrayList<>(collection);
						for (int i = 0; i < items.size(); i++) {
							var item = items.get(i);
							if (item != null) {
								ConstraintViolation.Path itemPath = (path == null
																	 ? ConstraintViolation.Path.ROOT
																	 : path).appendItem(i);
								try {
									for (Field field : value.getClass().getDeclaredFields()) {
										field.setAccessible(true);
										validateValue(field, field.getType(), field.get(item),
													  itemPath.appendField(field.getName()), consumer);
									}
								} catch (IllegalAccessException ex) {
									throw new HttpException(ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								}
							}
						}
					} else {
						try {
							for (Field field : value.getClass().getDeclaredFields()) {
								field.setAccessible(true);
								validateValue(field, field.getType(), field.get(value),
											  (path == null ? ConstraintViolation.Path.ROOT : path).appendField(field.getName()), consumer);
							}
						} catch (IllegalAccessException ex) {
							throw new HttpException(ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						}
					}
				}
			}
		}
	}

	private ConstraintViolation createViolation(ConstraintViolation.Path path, Annotation annotation, AnnotatedElement store, Object value) {
		String message = null;
		try {
			message = annotation.annotationType().getDeclaredMethod("message").invoke(annotation).toString();
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			message = null;
		}
		if (message == null) {
			message = annotation.annotationType().getName();
		} else {
			message = switch (message) {
				case "{javax.validation.constraints.NotNull.message}" -> "may not be null";
				case "{javax.validation.constraints.NotEmpty.message}" -> "may not be empty";
				case "{javax.validation.constraints.NotBlank.message}" -> "may not be blank";
				case "{javax.validation.constraints.Pattern.message}" -> "must match pattern '" + ((javax.validation.constraints.Pattern) annotation).regexp() + "'";
				default -> message;
			};
		}
		return new ConstraintViolation(message, path, store, value, (store instanceof Parameter) ? getParameterNameDescription((Parameter) store) : null);
	}

	private List<Predicate<Object>> getValidators(Annotation annotation) {
		return getAnnotationValidators(annotation).map(validatorClass -> {
			try {
				return validatorClass.getDeclaredConstructor().newInstance();
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
					 InvocationTargetException ex) {
				throw new HttpException(ex, 500);
			}
		}).map(validator -> {
			return (Predicate<Object>) new Predicate<Object>() {
				@Override
				public boolean test(Object it) {
					return validator.isValid(it, null);
				}
			};				
		}).toList();
	}

	private <A extends Annotation> Stream<Class<? extends ConstraintValidator<A,Object>>> getAnnotationValidators(A annotation) {
		Constraint constraint = annotation.annotationType().getAnnotation(Constraint.class);
		if (constraint != null) {
			var validatorClasses = (Class<? extends ConstraintValidator<A,Object>>[]) constraint.validatedBy();
			if (validatorClasses.length > 0) {
				return Stream.of(validatorClasses);
			}
		}
		return Stream.empty();
	}
	

	private void throwValidationError(Collection<ConstraintViolation> violations, Collection<ValidationException> parsingExceptions) {
		if (violations.isEmpty() && parsingExceptions.isEmpty()) {
			return;
		}
		throw new ExtendedValidationException(violations, parsingExceptions);
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
		try {
			Function<String, Object> mapper = DESERIALIZERS.get(expectedClass);
			if (mapper == null) {
				try {
					Method method = expectedClass.getDeclaredMethod("fromString", String.class);
					return method.invoke(null, valueStr);
				} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
					try {
						Method method = expectedClass.getDeclaredMethod("valueOf", String.class);
						return method.invoke(null, valueStr);
					} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
						// nothing to do..
						throw new ValidationException("Value '" + valueStr + "' cannot be converted to " + expectedClass.getCanonicalName(), ex);
					}
				}
			}
			return mapper.apply(valueStr);
		} catch (Throwable ex) {
			throw new ValidationException("Value '" + valueStr + "' cannot be converted to " + expectedClass.getCanonicalName(), ex);
		}
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
		if (producedContentTypes.isEmpty()) {
			return Optional.empty();
		}

		String header = request.getHeader("Accept");
		if (header == null) {
			return Optional.empty();
		}
		
		return Arrays.stream(header.split(",")).map(
				AcceptedType::new).filter(it -> producedContentTypes.contains(it.getMimeType())).sorted(Comparator.comparing(
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

	@Override
	public int compareTo(RequestHandler rh) {
		if (rh instanceof JaxRsRequestHandler) {
			JaxRsRequestHandler o = (JaxRsRequestHandler) rh;
			int r = PATTERN_COMPARATOR.compare(pattern, o.pattern);
			if (r != 0) {
				return r;
			}
			r = o.consumedContentTypes.size() - consumedContentTypes.size();
			if (r != 0) {
				return r;
			}
			r = o.producedContentTypes.size() - producedContentTypes.size();
			return r;
		}
		return Integer.MAX_VALUE;
	}
}
