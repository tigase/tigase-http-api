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
package tigase.http.modules.rest

import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.transform.CompileStatic
import tigase.http.coders.Coder
import tigase.http.coders.JsonCoder
import tigase.http.coders.XmlCoder
import tigase.http.rest.Handler
import tigase.http.util.CSSHelper

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

@CompileStatic
class OldGroovyResultEncoder {

	private static final Logger log = Logger.getLogger(OldGroovyResultEncoder.class.getCanonicalName());
	private Map<Handler,Map<String,Template>> handlerTemplates = new ConcurrentHashMap<>();
	private Map<String,Template> includes = new ConcurrentHashMap<>();
	private GStringTemplateEngine templateEngine = new GStringTemplateEngine();
	private final Coder xmlCoder = new XmlCoder();
	private final Coder jsonCoder = new JsonCoder();
	private List<Handler> allHandlers;

	public void loadTemplates(File rootScriptsDir, List<Handler> handlers) {
		allHandlers = handlers;
		List<String> methods = ["GET","POST","PUT","DELETE"];
		handlers.each { Handler handler ->
			File srcFile = ((Closure<File>)handler.getSourceFile).call();
			String name = srcFile.getName().replace(".groovy", "");
			Map<String,Template> templates = [ : ];
			methods.each { method ->
				try {
					String methodStr = method.toLowerCase().capitalize();

					if (handler.getProperty("exec$methodStr") == null) {
						return
					};

					File templateFile = new File(srcFile.getParentFile(), name + methodStr + ".html");
					if (!templateFile.exists()) {
						// in case we need one template file for every action
						templateFile = new File(srcFile.getParentFile(), name + ".html");
						if (!templateFile.exists()) {
							return
						};
					}

					templates[method] = templateEngine.createTemplate(templateFile.getText());
				} catch (Exception ex) {
					log.log(Level.WARNING, "could not load template for $srcFile for method $method", ex);
				}
			}
			if (templates.size() > 0) {
				handlerTemplates.put(handler, templates);
			}
		}

		[ "header", "footer", "index" ].each { src ->
			File f = Paths.get(rootScriptsDir.toString(), src + ".html").toFile();
			if (!f.exists()) {
				f = Paths.get(rootScriptsDir.getParentFile().toString(), src + '.html').toFile();
			}
			if (!f.exists()) {
				return
			};
			try {
				includes[src] = templateEngine.createTemplate(f.getText());
			} catch (Exception ex) {
				log.log(Level.WARNING, "could not load template for $src", ex);
			}
		}

		generateCodeExamples(handlers);
	}

	String getExpectedContentType(HttpServletRequest request) {
		String type = request.getParameter("type");
		if (type == null) {
			type = request.getHeader("Content-Type");
			if (type == null) {
				String acceptString = request.getHeader("Accept");
				if (acceptString && acceptString.startsWith("text/html")) {
					type = "text/html";
				}
			}
		}
		return type;
	}

	Template findTemplate(Handler route, String httpMethod) {
		Map<String,Template> templates = handlerTemplates[route];
		return (templates != null) ? templates.get(httpMethod) : null;
	}

	boolean canEncodeResults(Handler route, String httpMethod) {
		return findTemplate(route, httpMethod) != null;
	}

	def encodeResults(HttpServletRequest request, HttpServletResponse response, Handler route,
					  def result) {
		// send output data enconded with XML or JSON
		Template template = findTemplate(route, request.getMethod());
		fillResponseWithTemplate(route, template, request, response, result);
	}

	def renderIndex(HttpServletRequest request, HttpServletResponse response, def result) {
		Map<String,List<Handler>> handlersByPrefix = new HashMap<>();
		for (Handler handler : allHandlers) {
			String prefix = "/" + ((File) ((Closure<File>) handler.getSourceFile).call()).getParentFile().getName();
			List<Handler> tmp = handlersByPrefix.get(prefix);
			if (tmp == null) {
				tmp = new ArrayList<>();
				handlersByPrefix.put(prefix, tmp);
			}
			tmp.add(handler);
		}
		List<String> prefixes = handlersByPrefix.keySet().toSorted();
		((Map) result).put("handlersByPrefix", handlersByPrefix);
		((Map) result).put("prefixes", prefixes);
		Template template = includes["index"];
		fillResponseWithTemplate(null, template, request, response, result);
	}

	def fillResponseWithTemplate(Handler route, Template template, HttpServletRequest request, HttpServletResponse response,
								 def result) {
		response.setContentType("text/html");
		Map<String,Object> templateParams = null;
		def util = [ link: { String url ->
			if (request.getParameter("api-key")) {
				return request.getContextPath() + url + (url.contains("?") ? "&" : "?") + "api-key=" +
						request.getParameter("api-key");
			} else {
				return request.getContextPath() + url;
			}
		}, include       : { String name, Map params = null ->
			def temp = includes[name];
			if (temp == null) {
				return ""
			};
			Map<String,Object> map = new LinkedHashMap<>();
			map.put("imports", new ArrayList());
			map.putAll(templateParams);
			if (params != null) {
				map.putAll(params)
			};
			return temp.make(map);
		}, getFile       : { String name -> return new File(((File)((Closure<File>)route.getSourceFile).call()).getParentFile(), name);
		}, inlineCss     : { String path ->
			String content = CSSHelper.getCssFileContent(path);
			if (content == null) {
				return ""
			};
			return "<style>" + content + "</style>";
		} ];
		templateParams = [ request: request, response: response, result: result, util: util ];
		Writable writable = template.make(templateParams);
		response.setContentType("text/html");
		writable.writeTo(response.getWriter());
	}

	Map prepareParams(HttpServletRequest request, HttpServletResponse response, Map params) {
		params.util = [ formatData: {
			data -> return "*code*" + xmlCoder.encode(data).replace(" ", "&nbsp;").trim() + "*/code*";
		} ];
		return params;
	}

	def generateCodeExamples(List<Handler> handlers) {
		GStringTemplateEngine templateEngine = new GStringTemplateEngine();
		def params = [util: [ formatData: {
			data -> return "*code*" + xmlCoder.encode(data).replace(" ", "&nbsp;").trim() + "*/code*";
		} ]];

		for (Handler handler : handlers) {
			if (handler.generatedDescription != null) {
				continue;
			}

			if (handler.description != null) {
				handler.generatedDescription = [:];
				((Map) handler.description).forEach({ k,v ->
					def value = v;
					if (k != "regex") {
						value = [:];
						value.putAll((Map) v);
						if (value.description) {
							Template t = templateEngine.createTemplate((String) value.description);
							Writable writable = t.make(params);
							StringWriter sw = new StringWriter();
							writable.writeTo(sw);
							value.description = sw.toString().
									replace("<", "&lt;").
									replace(">", "&gt;").
									replace("\n", "<br/>").
									replace("*code*", "<div class='code'>").
									replace("*/code*", "</div>");
						}
					}
					((Map) handler.generatedDescription).put(k, value);
				});
			}
		}

	}
}
