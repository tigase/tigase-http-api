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
import org.jspecify.annotations.Nullable;
import tigase.http.api.HttpException;
import tigase.http.api.UnsupportedFormatException;
import tigase.http.jaxrs.marshallers.*;
import tigase.http.jaxrs.validators.ConstraintViolation;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
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
import java.time.LocalDate;
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


/**
 * <p>This class is responsible for managing the complete request processing lifecycle, including:
 * <ul>
 *   <li>Mapping incoming HTTP requests to appropriate handler methods based on URI patterns</li>
 *   <li>Enforcing role-based access control (RBAC) for secured endpoints</li>
 *   <li>Content type negotiation between client and server (via Accept and Content-Type headers)</li>
 *   <li>Parameter extraction and validation from various sources (path, query, header, form, body)</li>
 *   <li>Automatic serialization/deserialization of request/response payloads</li>
 *   <li>Support for synchronous and asynchronous request processing</li>
 * </ul>
 *
 * <p>The {@code JaxRsRequestHandler} extends the capabilities of the {@link RequestHandler} interface
 * and provides a comprehensive implementation that mirrors JAX-RS (Jakarta RESTful Web Services) behavior,
 * including support for standard annotations such as {@code @Path}, {@code @GET}, {@code @POST},
 * {@code @PUT}, {@code @DELETE}, {@code @Consumes}, {@code @Produces}, {@code @PathParam},
 * {@code @QueryParam}, {@code @HeaderParam}, {@code @FormParam}, and validation annotations.
 *
 * <h2>Key Responsibilities:</h2>
 *
 * <h3>1. Request Routing and Pattern Matching</h3>
 * <p>Each handler instance is associated with a compiled {@link Pattern} that matches specific URI paths.
 * The pattern may include path variables (e.g., {@code /users/{id}}) which are extracted and converted
 * to the appropriate Java types during request processing. The {@link #test(HttpServletRequest, String)}
 * method performs the URI matching and returns a {@link Matcher} if successful.</p>
 *
 * <h3>2. HTTP Method Handling</h3>
 * <p>Handlers are bound to specific HTTP methods (GET, POST, PUT, DELETE) as determined by method-level
 * annotations. Only requests with matching HTTP methods will be processed by a given handler.</p>
 *
 * <h3>3. Content Type Negotiation</h3>
 * <p>The handler supports both request content type validation (via {@code @Consumes}) and response
 * content type selection (via {@code @Produces}). The {@link #selectResponseMimeType(Method, HttpServletRequest)}
 * method evaluates the client's {@code Accept} header against the handler's produced content types,
 * selecting the best match based on quality factors (q-values).</p>
 *
 * <h3>4. Parameter Injection and Conversion</h3>
 * <p>The {@link #execute(HttpServletRequest, HttpServletResponse, Matcher, ScheduledExecutorService)}
 * method automatically extracts parameters from various sources and converts them to the target Java types:
 * <ul>
 *   <li><strong>Path Parameters:</strong> Extracted from URI path segments (e.g., {@code @PathParam("id")})</li>
 *   <li><strong>Query Parameters:</strong> Retrieved from URL query string (e.g., {@code @QueryParam("filter")})</li>
 *   <li><strong>Header Parameters:</strong> Extracted from HTTP headers (e.g., {@code @HeaderParam("Authorization")})</li>
 *   <li><strong>Form Parameters:</strong> Retrieved from form data in request body (e.g., {@code @FormParam("username")})</li>
 *   <li><strong>Request Body:</strong> Deserialized from request content based on Content-Type</li>
 *   <li><strong>Context Injection:</strong> Special types like {@link SecurityContext}, {@link UriInfo},
 *       {@link HttpServletRequest}, {@link HttpServletResponse}, and custom types like {@link Model}
 * 	   and {@link Pageable} are automatically injected</li>
 * </ul>
 * </p>
 *
 * <h3>5. Parameter Validation</h3>
 * <p>The handler performs comprehensive parameter validation using standard validation annotations
 * ({@code @NotNull}, {@code @NotEmpty}, {@code @NotBlank}, {@code @Pattern}, {@code @Valid}) and
 * custom validators implementing {@link ConstraintValidator}. Validation failures result in a
 * {@link ValidationException} with detailed constraint violation information.</p>
 *
 * <h3>6. Security and Access Control</h3>
 * <p>Handlers enforce role-based access control through {@link #requiredRole} and {@link #allowedRoles}.
 * The {@link #isAuthenticationRequired()} method indicates whether authentication is mandatory,
 * and access checks are performed before handler method invocation.</p>
 *
 * <h3>7. Response Handling</h3>
 * <p>The handler supports multiple response scenarios:
 * <ul>
 *   <li>Direct return of domain objects (automatically serialized to JSON/XML)</li>
 *   <li>Return of {@link Response} objects with custom status codes and headers</li>
 *   <li>Void methods that handle response writing directly</li>
 *   <li>Asynchronous responses via {@link AsyncResponseImpl} (annotated with {@code @Suspended})</li>
 * </ul>
 * </p>
 *
 * <h3>8. Error Handling</h3>
 * <p>The handler gracefully handles various error conditions:
 * <ul>
 *   <li>Parameter parsing failures ({@link ValidationException})</li>
 *   <li>Validation constraint violations ({@link ExtendedValidationException})</li>
 *   <li>Unsupported content types ({@link UnsupportedFormatException})</li>
 *   <li>Handler method exceptions (unwrapped via {@link #unwrapInvocationTargetException(Throwable)})</li>
 * </ul>
 * </p>
 *
 * <h2>Factory Methods:</h2>
 * <p>The class provides static factory methods for creating handler instances:
 * <ul>
 *   <li>{@link #create(Handler)} - Creates handlers for all annotated methods in a Handler instance</li>
 *   <li>{@link #create(String, Handler, Method)} - Creates a handler for a specific method</li>
 * </ul>
 * </p>
 *
 * <h2>Type Conversion Support:</h2>
 * <p>The handler supports automatic conversion of string parameters to various Java types through
 * the {@link #DESERIALIZERS} map and reflection-based discovery of {@code fromString()} and
 * {@code valueOf()} methods. Built-in support includes primitives, wrapper types, {@link String},
 * {@link BareJID}, {@link JID}, {@link LocalDate}, and collection types ({@link List}, {@link Set},
 * {@link SortedSet}).</p>
 *
 * <h2>Content Marshalling:</h2>
 * <p>Request and response payloads are marshalled/unmarshalled using content-type-specific implementations:
 * <ul>
 *   <li>JSON: {@link JsonMarshaller} / {@link JsonUnmarshaller}</li>
 *   <li>XML: {@link XmlMarshaller} / {@link XmlUnmarshaller}</li>
 *   <li>Form data: {@link WWWFormUrlEncodedUnmarshaller}</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety:</h2>
 * <p>Handler instances are designed to be thread-safe for concurrent request processing. However,
 * the underlying {@link Handler} instance must also be thread-safe as it may be invoked concurrently
 * by multiple requests.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Define a handler class
 * @Path("/api/users")
 * public class UserHandler implements Handler {
 *
 *     @GET
 *     @Path("/{id}")
 *     @Produces("application/json")
 *     public User getUser(@PathParam("id") Long id) {
 * 		 return userService.findById(id);
 *     }
 *
 *     @POST
 *     @Consumes("application/json")
 *     @Produces("application/json")
 *     public Response createUser(@Valid User user) {
 * 		 User created = userService.create(user);
 * 		 return Response.status(201).entity(created).build();
 *     }
 * }
 *
 * // Create handlers from the class
 * UserHandler handler = new UserHandler();
 * List<JaxRsRequestHandler> handlers = JaxRsRequestHandler.create(handler);
 * }</pre>
 *
 * @see RequestHandler
 * @see Handler
 * @see HttpMethod
 * @see Pattern
 * @see jakarta.ws.rs.Path
 * @see jakarta.ws.rs.GET
 * @see jakarta.ws.rs.POST
 * @see jakarta.ws.rs.PUT
 * @see jakarta.ws.rs.DELETE
 * @see jakarta.ws.rs.Consumes
 * @see jakarta.ws.rs.Produces
 * @see jakarta.ws.rs.PathParam
 * @see jakarta.ws.rs.QueryParam
 * @see jakarta.ws.rs.HeaderParam
 * @see jakarta.ws.rs.FormParam
 * @see javax.validation.Valid
 * @see javax.validation.constraints.NotNull
 * @see javax.validation.constraints.NotEmpty
 * @see javax.validation.constraints.NotBlank
 */
public class JaxRsRequestHandler
		implements RequestHandler {

	private static final Logger log = Logger.getLogger(JaxRsRequestHandler.class.getCanonicalName());
	private static final Map<Class, Function<String,Object>> DESERIALIZERS = new HashMap<>();
	public static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{[^}]+\\}?|[^{]+");
	static {
		DESERIALIZERS.put(long.class, Long::parseLong);
		DESERIALIZERS.put(int.class, Integer::parseInt);
		DESERIALIZERS.put(Long.class, Long::parseLong);
		DESERIALIZERS.put(Integer.class, Integer::parseInt);
		DESERIALIZERS.put(Double.class, Double::parseDouble);
		DESERIALIZERS.put(Float.class, Float::parseFloat);
		DESERIALIZERS.put(String.class, s -> {
			if (s == null || s.isEmpty()) {
				return null;
			} else {
				return s;
			}
		});
		DESERIALIZERS.put(BareJID.class, str -> {
			try {
				return BareJID.bareJIDInstance(str);
			} catch (TigaseStringprepException ex) {
				throw new IllegalArgumentException(ex.getMessage(), ex);
			}
		});
		DESERIALIZERS.put(LocalDate.class, str -> str == null || str.isEmpty() ? null : LocalDate.parse(str));
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

	/**
	 * Creates a list of {@link JaxRsRequestHandler} objects derived from the methods of the given
	 * {@code Handler} instance. Each {@code JaxRsRequestHandler} is linked to an individual handler method
	 * in the provided instance.
	 *
	 * @param instance the {@code Handler} instance whose methods will be analyzed and converted into
	 *                 {@link JaxRsRequestHandler} objects. Must not be {@code null}.
	 * @return a list of {@link JaxRsRequestHandler} objects corresponding to the methods of the provided
	 *         {@code Handler} instance. If no valid handler methods are found, an empty list is returned.
	 */
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

	/**
	 * Creates a {@code JaxRsRequestHandler} instance for the specified method of 
	 * the given {@code Handler} instance. The created handler is capable of handling
	 * HTTP requests that match the method's annotated path and HTTP method.
	 *
	 * @param contextPath the base URI path for the handler, used as the starting 
	 *                    point for constructing the full URI path. Must not be {@code null}.
	 * @param instance    the {@code Handler} instance containing the method to be 
	 *                    associated with the created {@code JaxRsRequestHandler}. 
	 *                    Must not be {@code null}.
	 * @param method      the {@code Method} instance representing the endpoint 
	 *                    handler method to link to the {@code JaxRsRequestHandler}. 
	 *                    This method must be public and annotated with the appropriate 
	 *                    HTTP method annotations like {@code GET}, {@code POST}, etc.
	 * @return a {@link JaxRsRequestHandler} object initialized with the provided instance, 
	 *         method, HTTP method, and computed URI pattern, or {@code null} if the 
	 *         method is invalid (e.g., not public or not associated with an HTTP method).
	 */
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
		log.log(Level.CONFIG, "Creating JaxRsRequestHandler for method: " + method.getName() + "; path: " + fullPath + "; httpMethod: " + httpMethod + "; methodPath = " + methodPath + "; allowedRoles = " + allowedRoles + "; pattern = " + pattern);
		return new JaxRsRequestHandler(instance, method, httpMethod, pattern, instance.getRequiredRole(), allowedRoles);
	}

	/**
	 * Determines the HTTP method associated with a given Java method by checking for specific HTTP method annotations.
	 *
	 * @param method the {@code Method} instance to inspect for HTTP method annotations. 
	 *               Must not be {@code null}.
	 * @return the {@link HttpMethod} corresponding to the annotation on the provided method, 
	 *         or {@code null} if no matching annotation is found.
	 */
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

	/**
	 * Determines whether authentication is required for the current request.
	 *
	 * This method checks if authentication is required either by delegating 
	 * to the superclass implementation or by verifying the presence of allowed roles.
	 *
	 * @return true if authentication is required; false otherwise.
	 */
	@Override
	public boolean isAuthenticationRequired() {
		return RequestHandler.super.isAuthenticationRequired() || allowedRoles != null;
	}

	/**
	 * Represents a segment of a path, which may be either a static literal
	 * or a dynamic parameter. A PathSegment is immutable and encapsulates the
	 * segment value, the associated regular expression, and whether the segment
	 * is a parameter.
	 *
	 * @param value   the string representation of the path segment.
	 * @param regex   the regular expression used for matching this segment, if it is a parameter.
	 * @param isParam a flag indicating whether this segment represents a parameter.
	 */
	record PathSegment(String value, String regex, boolean isParam) {
        /**
		 * Handles escaping of wildcards in string literal and insert properly formatted path parameter regexp.
		 *
         * @return properly escaped regex string
         */
		@Override
		public String toString() {
			if (isParam()) {
				return "(?<" + value + ">" + regex + ")";
			} else {
				return value.replace(".", "\\.").replace("+", "\\+");
			}
		}
	}

	/**
	 * Prepares a regular expression pattern matcher based on the provided path and method.
	 *
	 * @param path the input path containing placeholders for path variables
	 * @param method the method whose parameters will be used to validate path variables
	 * @return a compiled {@code Pattern} object representing the regular expression for the path,
	 *         or {@code null} if the path cannot be processed
	 */
	public static Pattern prepareMatcher(String path, Method method) {
		Map<String, Class> paramClasses = methodsPathParams(method);

		StringBuilder resultPattern = new StringBuilder();

		Matcher m = PATH_VARIABLE_PATTERN.matcher(path);
		while (m.find()) {
			var p = parseMatchedPartToPathSegment(m.group(), paramClasses);
			if (p == null) {
				return null;
			}
			resultPattern.append(p);
		}
		return Pattern.compile(resultPattern.toString());
	}

	/**
	 * Parses a matched part of a URL into a {@code PathSegment} object.
	 *
	 * @param group the match result containing the matched part of the URL. It is expected
	 *          to provide a valid group representation of the URL segment.
	 * @param paramClasses a map of parameter names to their corresponding classes,
	 *                     used to determine if a matched part is a parameter and what
	 *                     data type it represents.
	 * @return a {@code PathSegment} object representing the parsed segment if successful,
	 *         or {@code null} if the matched part is invalid, unbalanced, or references an
	 *         undefined parameter.
	 */
	private static @Nullable PathSegment parseMatchedPartToPathSegment(String group, Map<String, Class> paramClasses) {
		var isParam = group.startsWith("{");

		// check if the parameter is correct/balanced
		if (isParam  && !group.endsWith("}")) {
			 return null;
		}

		// drop braces if this is indeed the correct path parameter
		var value = isParam ? group.substring(1, group.length() - 1) : group;

		String regex = null;
		// if it has custom regex defined - extract it
		if (value.indexOf(':') > -1) {
			regex = value.substring(value.indexOf(':') + 1).trim();
			value = value.substring(0, value.indexOf(':')).trim();
		}

		// check if the parameter is defined for the method
		if (isParam && !paramClasses.containsKey(value)) {
			return null;
		}

		// if we don't have a custom regex defined - use default regex for the type
		if (regex == null && paramClasses.get(value) != null) {
			regex = regexForClass(paramClasses.get(value));
		}

		return new PathSegment(value, regex, isParam);
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

	/**
	 * Generates a regular expression string suitable for matching input based on the provided class type.
	 *
	 * <p>This method determines the appropriate regular expression pattern by examining the class type
	 * and its characteristics. The generated regex is used primarily for URL path parameter validation
	 * in JAX-RS-style request routing.</p>
	 *
	 * <p><strong>Supported Type Mappings:</strong></p>
	 * <ul>
	 *   <li><strong>Numeric types</strong> ({@code Long}, {@code Integer}, {@code int}, {@code long}):
	 * 	   Returns {@code "[0-9]+"} to match one or more digits.</li>
	 *   <li><strong>String type</strong> ({@code String}):
	 * 	   Returns {@code "[^\\/]+"} to match any characters except forward slashes.</li>
	 *   <li><strong>BareJID type</strong> ({@code BareJID}):
	 * 	   Returns {@code "[^\\/]+"} to match XMPP bare JID formats (excluding slashes).</li>
	 *   <li><strong>Custom types with conversion methods</strong>:
	 * 	   For classes that provide a static {@code fromString(String)} or {@code valueOf(String)} method,
	 * 	   returns {@code "[^\\/]+"} to allow general string matching.</li>
	 * </ul>
	 *
	 * <p><strong>Method Resolution Order:</strong></p>
	 * <ol>
	 *   <li>First attempts to locate a static {@code fromString(String)} method in the class</li>
	 *   <li>If not found, attempts to locate a static {@code valueOf(String)} method</li>
	 *   <li>If neither method exists, throws a {@code RuntimeException}</li>
	 * </ol>
	 *
	 * @param clazz the class type for which the regular expression is to be generated;
	 *              must not be {@code null}
	 * @return a regular expression string tailored to match valid input values for the specified class type;
	 * returns {@code null} if no suitable mapping can be determined
	 * @throws RuntimeException if the class type is not a recognized primitive, wrapper, or standard type,
	 *                          and neither {@code fromString} nor {@code valueOf} static methods are found for string-to-object
	 *                          conversion. This indicates the class cannot be used as a path parameter without a custom
	 *                          conversion mechanism.
	 * @see PathParam
	 * @see #prepareMatcher(String, Method)
	 */
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

	/**
	 * Extracts a mapping of path parameter names to their corresponding types from the given method's parameters.
	 *
	 * @param method the method whose parameters will be analyzed for PathParam annotations
	 * @return a map where the keys are the path parameter names and the values are their corresponding types
	 */
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

	/**
	 * Tests if a given HTTP request matches certain criteria based on its content type,
	 * Accept header, and URI pattern.
	 *
	 * <p>This method evaluates whether the incoming HTTP request can be handled by this
	 * {@code JaxRsRequestHandler} by performing the following checks:</p>
	 *
	 * <ol>
	 *   <li><strong>Content Type Validation:</strong> Verifies that the request's content type
	 * 	   is either empty or matches one of the content types that this handler consumes
	 * 	   (as defined by the {@code @Consumes} annotation on the handler method).</li>
	 *   <li><strong>Accept Header Validation:</strong> If the request includes an {@code Accept}
	 * 	   header and this handler produces specific content types (as defined by the
	 *       {@code @Produces} annotation), the method checks if any of the accepted types
	 * 	   match the produced types. The matching is done by parsing the Accept header,
	 * 	   creating {@code AcceptedType} objects with preference values, and selecting
	 * 	   the best match based on quality factors (q-values).</li>
	 *   <li><strong>URI Pattern Matching:</strong> If the content type and Accept header
	 * 	   validations pass, the method attempts to match the request URI against this
	 * 	   handler's predefined URI pattern (which may include path parameters).</li>
	 * </ol>
	 *
	 * <p><strong>Return Value:</strong></p>
	 * <ul>
	 *   <li>Returns a {@link Matcher} object if all validation criteria are satisfied and
	 * 	   the URI pattern matches the request URI. This {@code Matcher} can be used to
	 * 	   extract path parameters from the matched URI.</li>
	 *   <li>Returns {@code null} if any of the following conditions occur:
	 * 	 <ul>
	 * 	   <li>The request's content type does not match any consumed content types</li>
	 * 	   <li>The Accept header is present but none of its values match the produced content types</li>
	 * 	   <li>The request URI does not match the handler's pattern</li>
	 * 	 </ul>
	 *   </li>
	 * </ul>
	 *
	 * <p><strong>Example Usage:</strong></p>
	 * <pre>
	 * // Handler method annotated with @Consumes("application/json") and @Produces("application/json")
	 * Matcher matcher = requestHandler.test(request, "/api/users/123");
	 * if (matcher != null && matcher.matches()) {
	 * 	 String userId = matcher.group("id"); // Extract path parameter
	 * 	 // Process request...
	 * }
	 * </pre>
	 *
	 * @param request    the {@code HttpServletRequest} object containing the client's request information,
	 *                   including headers (Content-Type, Accept) and the request URI. Must not be {@code null}.
	 * @param requestUri the URI string of the request to be matched against this handler's predefined
	 *                   URI pattern. This should be the normalized path without query parameters.
	 *                   Must not be {@code null}.
	 * @return a {@code Matcher} object if the request URI matches the pattern and all criteria
	 * for content type and Accept header are satisfied; {@code null} otherwise, indicating
	 * that this handler cannot process the request.
	 * @see Matcher
	 * @see #getPattern()
	 * @see AcceptedType
	 * @see jakarta.ws.rs.Consumes
	 * @see jakarta.ws.rs.Produces
	 */
	public Matcher test(HttpServletRequest request, String requestUri) {
		if (consumedContentTypes.isEmpty() || request.getContentType() == null || consumedContentTypes.contains(request.getContentType())) {
			String header = request.getHeader("Accept");
			if (header != null && !producedContentTypes.isEmpty()) {
				if (Arrays.stream(header.split(",")).map(AcceptedType::new)
						.noneMatch(it -> producedContentTypes.stream().anyMatch(it::matches))) {
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

	/**
	 * Executes a complex HTTP request-processing pipeline. This method is the core entry point
	 * for handling all incoming HTTP requests that match this handler's pattern and HTTP method.
	 *
	 * <p><strong>Request Processing Lifecycle:</strong></p>
	 * <p>The execution follows a well-defined sequence of operations:</p>
	 * <ol>
	 *   <li><strong>Context Initialization:</strong> Creates a {@link ContainerRequestContext}
	 * 	   wrapping the servlet request to provide JAX-RS-style access to request metadata.</li>
	 *
	 *   <li><strong>Content Negotiation:</strong> Determines the response MIME type by analyzing
	 * 	   the client's {@code Accept} header against the handler's {@code @Produces} annotations,
	 * 	   selecting the best match based on quality factors (q-values).</li>
	 *
	 *   <li><strong>Parameter Extraction:</strong> Iterates through the handler method's parameters
	 * 	   and extracts values from various sources:
	 * 	   <ul>
	 * 		 <li>{@code @PathParam} - Extracted from URI path segments using the {@link Matcher}</li>
	 * 		 <li>{@code @QueryParam} - Retrieved from URL query string parameters</li>
	 * 		 <li>{@code @HeaderParam} - Extracted from HTTP request headers</li>
	 * 		 <li>{@code @FormParam} - Retrieved from form data (application/x-www-form-urlencoded
	 * 			 or multipart/form-data), with special handling for {@code boolean} (on/off values),
	 *             {@link InputStream}, and {@link Part} types</li>
	 * 		 <li>{@code @BeanParam} - Unmarshalled from form data using {@link WWWFormUrlEncodedUnmarshaller}</li>
	 * 		 <li>{@code @Suspended} - Creates an {@link AsyncResponseImpl} for asynchronous processing</li>
	 * 		 <li><strong>Request Body:</strong> Deserialized based on the {@code Content-Type} header
	 * 			 using appropriate unmarshallers (JSON, XML, or form data)</li>
	 * 		 <li><strong>Context Injection:</strong> Special parameters are automatically injected:
	 * 		   <ul>
	 * 			 <li>{@link Pageable} - Pagination metadata from query parameters</li>
	 * 			 <li>{@link SecurityContext} - Security and authentication context</li>
	 * 			 <li>{@link HttpServletRequest} - Raw servlet request object</li>
	 * 			 <li>{@link HttpServletResponse} - Raw servlet response object</li>
	 * 			 <li>{@link UriInfo} - URI and path information</li>
	 * 			 <li>{@link Model} - View model for template rendering</li>
	 * 		   </ul>
	 * 		 </li>
	 * 	   </ul>
	 *   </li>
	 *
	 *   <li><strong>Type Conversion:</strong> String values are converted to target parameter types
	 * 	   using built-in converters (for primitives, wrappers, dates, JIDs) or reflectively discovered
	 *       {@code fromString()} / {@code valueOf()} methods. Collections ({@link List}, {@link Set},
	 *       {@link SortedSet}) are supported for multi-valued parameters.</li>
	 *
	 *   <li><strong>Parameter Validation:</strong> Performs comprehensive validation using:
	 * 	   <ul>
	 * 		 <li>Standard annotations: {@code @NotNull}, {@code @NotEmpty}, {@code @NotBlank},
	 *             {@code @Pattern}</li>
	 * 		 <li>Nested object validation via {@code @Valid} annotation (recursive field traversal)</li>
	 * 		 <li>Custom validators implementing {@link ConstraintValidator}</li>
	 * 		 <li>Default value injection via {@code @DefaultValue} for missing parameters</li>
	 * 	   </ul>
	 * 	   Validation failures are collected and reported as {@link ExtendedValidationException}
	 * 	   with detailed constraint violation information.
	 *   </li>
	 *
	 *   <li><strong>Handler Method Invocation:</strong> Reflectively invokes the target handler method
	 * 	   with the extracted and validated parameters. The method is executed within a try-catch block
	 * 	   that unwraps {@link InvocationTargetException} to expose the underlying cause.</li>
	 *
	 *   <li><strong>Response Handling:</strong> Processes the handler method's return value:
	 * 	   <ul>
	 * 		 <li>{@code void} methods - Returns immediately without writing response body</li>
	 * 		 <li>{@code null} return - Sets HTTP 200 status without body</li>
	 * 		 <li>{@link Response} objects - Copies status, headers, and entity to servlet response</li>
	 * 		 <li>Domain objects - Automatically marshalled to JSON/XML based on selected MIME type</li>
	 * 	   </ul>
	 *   </li>
	 *
	 *   <li><strong>Asynchronous Processing:</strong> If an {@link AsyncResponseImpl} was created
	 * 	   (via {@code @Suspended} parameter), exceptions during processing are forwarded to
	 *       {@link AsyncResponseImpl#resume(Throwable)} for asynchronous error handling.</li>
	 * </ol>
	 *
	 * <p><strong>Error Handling Strategy:</strong></p>
	 * <ul>
	 *   <li><strong>Validation Errors:</strong> Parameter parsing failures ({@link ValidationException})
	 * 	   are collected alongside constraint violations and thrown as a single
	 *       {@link ExtendedValidationException} before method invocation.</li>
	 *
	 *   <li><strong>Content Type Errors:</strong> Unsupported {@code Content-Type} or {@code Accept}
	 * 	   headers result in {@link UnsupportedFormatException} (HTTP 422).</li>
	 *
	 *   <li><strong>Handler Exceptions:</strong> Exceptions thrown by the handler method are unwrapped
	 * 	   via {@link #unwrapInvocationTargetException(Throwable)} and rethrown with appropriate
	 * 	   HTTP status codes:
	 * 	   <ul>
	 * 		 <li>{@link HttpException} - Preserves original HTTP status code and message</li>
	 * 		 <li>{@link TigaseStringprepException} - Converted to {@link ValidationException}</li>
	 * 		 <li>Other exceptions - Wrapped in {@link HttpException} with status 500</li>
	 * 	   </ul>
	 *   </li>
	 *
	 *   <li><strong>Asynchronous Errors:</strong> If asynchronous processing is active, all exceptions
	 * 	   are delegated to {@link AsyncResponseImpl#resume(Throwable)} to prevent servlet container
	 * 	   error handling interference.</li>
	 * </ul>
	 *
	 * <p><strong>Thread Safety and Context Management:</strong></p>
	 * <p>The method uses thread-local context management via {@link ContainerRequestContext}:
	 * <ul>
	 *   <li>{@code setContext(context)} - Stores context in thread-local for handler method access</li>
	 *   <li>{@code resetContext()} - Clears thread-local in {@code finally} block to prevent leaks</li>
	 * </ul>
	 * This ensures proper cleanup even when exceptions occur, preventing context pollution across requests.
	 * </p>
	 *
	 * <p><strong>Example Execution Flow:</strong></p>
	 * <pre>
	 * // Request: POST /api/users with JSON body and Accept: application/json
	 * // Handler: @POST @Path("/users") @Consumes("application/json") @Produces("application/json")
	 * //		  public Response createUser(@Valid User user)
	 *
	 * 1. Create ContainerRequestContext for request metadata
	 * 2. Select "application/json" as response MIME type (matches Accept header)
	 * 3. Extract User object from JSON request body using JsonUnmarshaller
	 * 4. Validate User object (check @NotNull, @NotEmpty fields recursively)
	 * 5. Invoke createUser(user) on handler instance
	 * 6. Process Response object (extract status 201, headers, entity)
	 * 7. Marshal entity to JSON and write to response
	 * </pre>
	 *
	 * <p><strong>Performance Considerations:</strong></p>
	 * <ul>
	 *   <li>Parameter extraction is performed sequentially; avoid excessive parameters</li>
	 *   <li>Validation is recursive for {@code @Valid} annotated objects; deep nesting impacts performance</li>
	 *   <li>Reflection-based method invocation has overhead; consider caching for high-frequency endpoints</li>
	 *   <li>Asynchronous processing offloads request thread but requires executor service capacity</li>
	 * </ul>
	 *
	 * @param request         The {@link HttpServletRequest} representing the HTTP request.
	 *                        Provides access to request URI, headers, parameters, and body content.
	 *                        Must not be {@code null}.
	 * @param response        The {@link HttpServletResponse} used to generate HTTP responses.
	 *                        Enables setting response status codes, headers, and body content.
	 *                        Must not be {@code null}.
	 * @param matcher         A {@link Matcher} instance that has successfully matched the request URI
	 *                        against this handler's path pattern. Used to extract path parameter values
	 *                        via named capturing groups (e.g., {@code matcher.group("id")}).
	 *                        Must not be {@code null} and must be in a matched state.
	 * @param executorService The {@link ScheduledExecutorService} used for scheduling asynchronous
	 *                        request processing tasks and timeout handlers when
	 *                        {@link AsyncResponseImpl} is used. Must not be {@code null}.
	 * @throws HttpException If an HTTP-specific error occurs during request processing:
	 *                        <ul>
	 *                       <li>HTTP 422 - Unsupported content type or deserialization failure</li>
	 *                       <li>HTTP 406 - Validation constraint violations or parameter parsing errors</li>
	 *                       <li>HTTP 500 - Handler method invocation errors or internal processing failures</li>
	 *                       <li>Custom codes - Propagated from {@link HttpException} thrown by handler methods</li>
	 *                        </ul>
	 * @throws IOException   If an input or output error occurs while reading the request body
	 *                       (via {@link HttpServletRequest#getInputStream()}) or writing the
	 *                       response body (via {@link HttpServletResponse#getOutputStream()}).
	 *                       This typically indicates network issues or client disconnection.
	 * @see #test(HttpServletRequest, String)
	 * @see #selectResponseMimeType(Method, HttpServletRequest)
	 * @see #convertToValue(Type, String[])
	 * @see #validateParameters(Object, Method, Object[], Predicate, Consumer)
	 * @see #sendEncodedContent(Object, Optional, HttpServletResponse)
	 * @see ContainerRequestContext
	 * @see AsyncResponseImpl
	 * @see ExtendedValidationException
	 */
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
						} else if (InputStream.class.isAssignableFrom(param.getType())) {
							try {
								Part part = request.getPart(formParam.value());
								if (part != null) {
									value = part.getInputStream();
								}
							} catch (ServletException e) {
								throw new RuntimeException(e);
							}
						} else if (Part.class.equals(param.getType())) {
							try {
								value = request.getPart(formParam.value());
							} catch (ServletException e) {
								throw new RuntimeException(e);
							}
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
						if (valueStr != null && valueStr.isEmpty()) {
							return null;
						}
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
		
		return Arrays.stream(header.split(","))
				.map(AcceptedType::new)
				.sorted(Comparator.comparing(AcceptedType::getPreference).reversed())
				.flatMap(it -> producedContentTypes.stream()
						.filter(it::matches)
						.sorted(Comparator.comparingInt(p -> p.contains("*") ? 1 : 0)))
				.findFirst();
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("JaxRsRequestHandler{");
		sb.append("requiredRole=").append(getRequiredRole());
		sb.append(", allowedRoles=").append(getAllowedRoles());
		sb.append(", handler=").append(getHandler());
		sb.append(", method=").append(getMethod().getName());
		sb.append(", pattern=").append(getPattern());
		sb.append(", httpMethod=").append(getHttpMethod());
		sb.append(", authenticationRequired=").append(isAuthenticationRequired());
		sb.append(", consumedContentTypes=").append(consumedContentTypes);
		sb.append(", producedContentTypes=").append(producedContentTypes);
		sb.append('}');
		return sb.toString();
	}
}
