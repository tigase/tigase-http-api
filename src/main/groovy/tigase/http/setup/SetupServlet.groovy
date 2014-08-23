/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tigase.http.setup;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import groovy.transform.CompileStatic
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author andrzej
 */
@CompileStatic
public class SetupServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(SetupServlet.class.getCanonicalName());
	
	private final TemplateEngine templateEngine = new GStringTemplateEngine();
	
	private final Map<String,Template> templates = new ConcurrentHashMap<>();
	
	private final Map config = [test:1];
	
	@Override
    public void init() throws ServletException {
		super.init();
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
		
		loadTemplates();
		
		String i = request.getParameter("step");
		if (i == null || i.isEmpty()) {
			i = "1";
		}
		Template t = templates.get("step" + i);
		Map templateParams = null;
		Map util = [
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
				];
		templateParams = [request:request, response:response, servlet:this, util:util, config:config, currentStep:i];
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
				log.log(Level.SEVERE, "loaded step " + i);
			} catch (Exception ex) {
				ex.printStackTrace();
				loaded = false;
			}
			i++;
		}
		["header", "footer", "index"].each { String file ->
			try {
				String templateSrc = load(file, null, "html");
				Template template = templateEngine.createTemplate(templateSrc);
				templates.put(file, template);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private String load(String prefix, Integer i, String suffix) throws IOException {
		String path = "tigase/setup/" + prefix + (i == null ? "" : ("-" + i)) + "." + suffix;
		InputStream is = getClass().getResourceAsStream(path);
		if (is == null) {
			log.log(Level.SEVERE, "trying to load file " + new File(path).getAbsolutePath());
			is = new FileInputStream(new File(path));
		}
		if (is == null)
			throw new RuntimeException("Resource not found");
		
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
