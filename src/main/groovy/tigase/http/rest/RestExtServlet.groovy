/*
 * Tigase HTTP API
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.http.rest

import java.util.logging.Level;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 *
 * @author andrzej
 */
class RestExtServlet extends RestServlet {
	
	def templateEngine = new GStringTemplateEngine();
	def handlerTemplates = [:];
	
	def includes = [:];
	
	@Override
	public void loadHandlers(File[] scriptFiles) {
		super.loadHandlers(scriptFiles);
		def allHandlers = [];
		this.handlers.each { method, handlers ->
			handlers.each { handler ->
				if (!allHandlers.contains(handler)) {
					allHandlers.add(handler);
				}
			}
		}
		File srcParent = scriptFiles[0].getParentFile();
		allHandlers.each { Handler handler ->
			File srcFile = handler.getSourceFile();
			String name = srcFile.getName().replace(".groovy", "");
			def templates = [:];
			methods.each { method ->
				try {
					String methodStr = method.toLowerCase().capitalize();
					if (handler."exec${methodStr}" == null)
						return;
					File templateFile = new File(name + methodStr + ".html", srcParent);
					if (!templateFile.exists()) 
						return;
					
					templates[method] = templateEngine.createTemplate(templateFile.getText());
				} catch (Exception ex) {
					log.log(Level.WARN, "could not load template for $srcFile for method $method", ex);
				}
			}
			if (templates.size() > 0) {
				handlerTemplates[handler] = templates;
			}
		}

		
		["header", "footer"].each { src ->
			File f = new File(src+".html", srcParent);
			if (!f.exists()) {
				f = new File(src+'.html', srcParent.getParentFile());
			}
			if (!f.exists()) return;
			try {
				includes[src] = templateEngine.createTemplate(f.getText());
			} catch (Exception ex) {
				log.log(Level.WARN, "could not load template for $src", ex);
			}
		}
	}
	
	@Override
	def encodeResults(HttpServletRequest request, HttpServletResponse response, Handler route, def reqParams, def result ) { 
		// send output data enconded with XML or JSON
		String type = request.getParameter("type");
		if (type == null) {
			String acceptString = request.getHeader("Accept");
			if (acceptString && acceptString.startsWith("text/html")) {
				type = "text/html";
			}
		}
		if (type == "text/html") {
			def templates = handlerTemplates[route];
			if (templates && templates[request.getMethod()]) {
				Template template = (Template) templates[request.getMethod()];
				def templateParams = null;
				def util = [
				link: { url ->
					if (request.getParameter("api-key")) {
						return request.getContextPath() + url + (url.contains("?") ? "&" : "?") + "api-key=" + request.getParameter("api-key");
					} else {
						return request.getContextPath() + url;
					}
				}, include: { name, params = null ->
					def temp = includes[name];
					if (temp == null)
						return "";
					def map = [:];
					map.putAll(templateParams);
					if (params != null) map.putAll(params);
					return temp.make(map);
				}				
				];
				templateParams = [request:request, response:response, result:result, util:util];
				Writable writable = template.make(templateParams);
				response.setContentType(type);
				writable.writeTo(response.getWriter());
				return;
			}
		}
		super.encodeResults(request, response, route, reqParams, result);
	}
	
}

