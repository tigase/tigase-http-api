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
package tigase.http.modules.setup

import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.text.TemplateEngine
import groovy.transform.CompileStatic
import tigase.db.UserRepositoryMDImpl
import tigase.http.modules.AbstractBareModule

import javax.servlet.ServletConfig
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
/**
 *
 * @author andrzej
 */
@CompileStatic
public class SetupServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(SetupServlet.class.getCanonicalName());
	
	private final TemplateEngine templateEngine = new GStringTemplateEngine();
	
	private final Map<String,Template> templates = new ConcurrentHashMap<>();
	
	//private final Map config = [test:1];
	private final Setup setup = new Setup();
	
	private SetupModule setupModule;

	@Override
    public void init() throws ServletException {
		super.init();
		ServletConfig cfg = super.getServletConfig();
		String moduleUUID = cfg.getInitParameter("module");
		setupModule = (SetupModule) AbstractBareModule.getModuleByUUID(moduleUUID);
		loadTemplates();
	}
	
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            processRequest(request, response);
        }
        catch (Exception ex) {
            log.log(Level.SEVERE, "exception processing request", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
	
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		if (!request.isUserInRole('admin') && !request.authenticate(response)) {
			return;
		}

		loadTemplates();

		Template t = null;
		Map templateParams = [request:request, response:response, servlet:this];
		if (setupModule.getUserRepository() == null || ((setupModule.getUserRepository() instanceof UserRepositoryMDImpl) && !((UserRepositoryMDImpl) setupModule.getUserRepository()).getRepo(null)))  {
			templateParams["setup"] = setup;
			String step  = request.getParameter("step");
			int i = 1;
			if (step != null) {
				i = Integer.parseInt(step);
			}

			t = templates.get("step" + i);
			if (i > 1 && "POST".equals(request.getMethod())) {
				Setup.Page page = setup.getPage(i-1);
				page.setValues(request.getParameterMap());
			}
			templateParams.put("page", setup.getPage(i));
			templateParams.put("currentStep", i);
		} else {
			t = templates.get("edit");
			templateParams.put("currentStep", "edit");
		}
		templateParams.put("util", [
				link: { String url ->
					if (request.getParameter("api-key")) {
						return request.getContextPath() + url + (url.contains("?") ? "&" : "?") + "api-key=" + request.getParameter("api-key");
					} else {
						return request.getContextPath() + url;
					}
				}, include: { String name, Map params = null ->
					def temp = templates[name];
					if (temp == null)
						return "";
					def map = [:];
					map.putAll(templateParams);
					if (params != null) map.putAll(params);
					return temp.make(map);
				}				
				]);
		Writable w = t.make(templateParams);
		
		w.writeTo(response.getWriter());
	}
	
	
	private void loadTemplates() {
		int i=1;
		boolean loaded = true;
		while (loaded) {
			try {
				String templateSrc = load("step", i, "html");
				Template template = templateEngine.createTemplate(templateSrc);
				templates.put("step" + i, template);
				log.log(Level.FINEST, "loaded html template for step " + i);
			} catch (Exception ex) {
				log.log(Level.FINEST, "resource file for index = " + i + " was not found and could not be loaded", ex);
				loaded = false;
			}
			i++;
		}
		["header", "footer", "index", "edit"].each { String file ->
			try {
				String templateSrc = load(file, null, "html");
				Template template = templateEngine.createTemplate(templateSrc);
				templates.put(file, template);
			} catch (Exception ex) {
				log.log(Level.FINEST, "resource " + file + " was not found and could not be loaded", ex);
			}
		}
	}
	
	private String load(String prefix, Integer i, String suffix) throws IOException {
		String path = "tigase/setup/" + prefix + (i == null ? "" : ("-" + i)) + "." + suffix;
		File f = new File(path);
		InputStream is = null;
		if (f.exists()) {
			is = new FileInputStream(new File(path));
		} else {
			is = getClass().getResourceAsStream("/"+path);
		}
		if (is == null)
			throw new IOException("Resource not found");
		
		char[] buf = new char[1024];
		StringBuilder sb = new StringBuilder();
		Reader r = new InputStreamReader(is);
		try {
			int read;
			while ((read = r.read(buf)) > -1) {
				sb.append(buf, 0, read);
			}
		} finally {
			r.close();
		}
		return sb.toString();
	}
}
