package tigase.http.jaxrs;

import jakarta.ws.rs.*;
import tigase.http.api.HttpException;
import tigase.http.modules.AbstractBareModule;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class JaxRsServlet<H extends Handler, M extends JaxRsModule<H>> extends HttpServlet {

	public static String MODULE_KEY = "module-uuid";

	private ScheduledExecutorService executorService;
	protected M module;
	private ConcurrentHashMap<HttpMethod, CopyOnWriteArrayList<RequestHandler<H>>> requestHandlers = new ConcurrentHashMap<>();

	@Override
	public void init() throws ServletException {
		super.init();
		ServletConfig cfg = super.getServletConfig();
		module = AbstractBareModule.getModuleByUUID(cfg.getInitParameter(MODULE_KEY));
		executorService = module.getExecutorService();
		List<H> handlers = module.getHandlers();
		for (H handler : handlers) {
			Path path = handler.getClass().getAnnotation(Path.class);
			if (path == null) {
				continue;
			}

			registerHandler(path.value(), handler);
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			HttpMethod httpMethod = HttpMethod.valueOf(req.getMethod());
			List<RequestHandler<H>> handlers = Optional.ofNullable((List<RequestHandler<H>>) requestHandlers.get(httpMethod))
					.orElse(Collections.emptyList());
			String requestUri = req.getRequestURI();
			if (!req.getContextPath().equals("/")) {
				if (!req.getContextPath().isEmpty()) {
					requestUri = requestUri.substring(req.getContextPath().length());
				}
			}
			for (RequestHandler handler : handlers) {
				Matcher matcher = handler.test(req, requestUri);
				if (matcher != null && matcher.matches()) {
					canAccess(handler, req, resp);
					handler.execute(req, resp, matcher, executorService);
					return;
				}
			}
			resp.sendError(404, "Not found");
		} catch (HttpException ex) {
			resp.sendError(ex.getCode(), ex.getMessage());
		}
	}

	protected abstract void canAccess(RequestHandler<H> requestHandler, HttpServletRequest request,
									  HttpServletResponse response) throws HttpException, IOException, ServletException;

	protected void registerHandler(String rootPath, H handler) {
		Method[] methods = handler.getClass().getDeclaredMethods();
		for (Method method : methods) {
			if (!Modifier.isPublic(method.getModifiers())) {
				continue;
			}

			HttpMethod httpMethod = getHttpMethod(method);
			if (httpMethod == null) {
				continue;
			}

			String fullPath = rootPath;
			Path pathAnnotation = method.getAnnotation(Path.class);
			if (pathAnnotation != null) {
				if (!pathAnnotation.value().startsWith("/")) {
					fullPath = fullPath + "/";
				}

				fullPath = fullPath + pathAnnotation.value();
			}
			
			Pattern pattern = prepareMatcher(fullPath, method);

			CopyOnWriteArrayList<RequestHandler<H>> handlers = requestHandlers.computeIfAbsent(httpMethod, x -> new CopyOnWriteArrayList<>());
			handlers.add(new RequestHandler(handler, method, httpMethod, pattern));
		}

	}

	private HttpMethod getHttpMethod(Method method) {
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

	private Pattern prepareMatcher(String path, Method method) {
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

	private class Param {
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

	private String regexForClass(Class clazz) {
		if (Long.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
			return "[0-9]+";
		}
		if (String.class.isAssignableFrom(clazz)) {
			return "[^\\/]+";
		}
		try {
			System.out.println("checking if class " + clazz + " has fromString(String) method");
			Method m = clazz.getDeclaredMethod("fromString", String.class);
			if (Modifier.isStatic(m.getModifiers())) {
				System.out.println("method found");
				return "[^\\/]+";
			}
			System.out.println("method not static");
		} catch (NoSuchMethodException e) {
			System.out.println("method not found");
			// nothing to do..
		}
		return null;
	}

	private Map<String,Class> methodsPathParams(Method method) {
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

}
