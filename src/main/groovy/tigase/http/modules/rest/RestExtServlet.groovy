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
package tigase.http.modules.rest

import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.transform.CompileStatic
import tigase.http.rest.Handler
import tigase.http.util.CSSHelper

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.logging.Level
import java.util.logging.Logger

/**
 *
 * @author andrzej
 */
class RestExtServlet
		extends RestServlet {

	Logger log = Logger.getLogger(RestExtServlet.class.getCanonicalName())

	GStringTemplateEngine templateEngine = new GStringTemplateEngine();
	def handlerTemplates = [ : ];

	def includes = [ : ];

	@Override
	public void loadHandlers(File[] scriptFiles) {
		super.loadHandlers(scriptFiles);
		def allHandlers = [ ];
		this.handlers.each { method, handlers ->
			handlers.each { handler ->
				if (!allHandlers.contains(handler)) {
					allHandlers.add(handler);
				}
			}
		}
		if (scriptFiles.length > 0) {
			//File srcParent = scriptFiles[0].getParentFile();
			allHandlers.each { Handler handler ->
				File srcFile = handler.getSourceFile();
				String name = srcFile.getName().replace(".groovy", "");
				def templates = [ : ];
				methods.each { method ->
					try {
						String methodStr = method.toLowerCase().capitalize();
						if (handler."exec${methodStr}" == null) {
							return
						};
						File templateFile = new File(name + methodStr + ".html", scriptsDir);
						if (!templateFile.exists()) {
							// in case we need one template file for every action
							templateFile = new File(name + ".html", scriptsDir);
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
					handlerTemplates[handler] = templates;
				}
			}
		}

		[ "header", "footer", "index" ].each { src ->
			File f = new File(src + ".html", scriptsDir);
			if (!f.exists()) {
				f = new File(src + '.html', scriptsDir.getParentFile());
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

		generateCodeExamples(allHandlers);
	}

	@Override
	def encodeResults(HttpServletRequest request, HttpServletResponse response, Handler route, def reqParams,
					  def result) {
		// send output data enconded with XML or JSON
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
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "got result for request with type = " + type);
		}
		if (type == "text/html" || type == 'application/x-www-form-urlencoded') {
			def templates = handlerTemplates[route];
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"looking for template for " + route + " and method " + request.getMethod() + " got " +
								(templates ? templates[request.getMethod()] : null) + " from " +
								handlerTemplates.size());
			}
			if (templates && templates[request.getMethod()]) {
				Template template = (Template) templates[request.getMethod()];
				fillResponseWithTemplate(template, request, response, result);
				return;
			}
		}
		super.encodeResults(request, response, route, reqParams, result);
	}

	def fillResponseWithTemplate(Template template, HttpServletRequest request, HttpServletResponse response,
								 def result) {
		def templateParams = null;
		def util = [ link: { url ->
			if (request.getParameter("api-key")) {
				return request.getContextPath() + url + (url.contains("?") ? "&" : "?") + "api-key=" +
						request.getParameter("api-key");
			} else {
				return request.getContextPath() + url;
			}
		}, include       : { name, params = null ->
			def temp = includes[name];
			if (temp == null) {
				return ""
			};
			def map = [ imports: [ ] ];
			map.putAll(templateParams);
			if (params != null) {
				map.putAll(params)
			};
			return temp.make(map);
		}, getFile       : { name -> return new File(scriptsDir, name);
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

	@CompileStatic
	def processRequest(HttpServletRequest request, HttpServletResponse response) {
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("comparing request URI = " + request.getRequestURI() + " with " +
								   (request.getContextPath() + request.getServletPath()));
			}
			String uri = request.getRequestURI();
			if (uri.equals(request.getContextPath() + request.getServletPath()) &&
					"/".equals(request.getServletPath())) {
				// accessing root of REST service - we should provide info about service here
				Template template = (Template) includes["index"];

				def result = [ service: service ];
				fillResponseWithTemplate(template, request, response, result);
				return;
			}
			super.processRequest(request, response);
		} catch (IOException ex) {
			throw new IOException(ex);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	def generateCodeExamples(def handlers) {
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
						value.putAll(v);
						if (value.description) {
							Template t = templateEngine.createTemplate(value.description);
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
					handler.generatedDescription[k] = value;
				});
			}
		}
		
	}
	
}

