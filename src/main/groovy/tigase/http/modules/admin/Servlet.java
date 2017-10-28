/*
 * Servlet.java
 *
 * Tigase HTTP API
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.modules.admin;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import org.codehaus.groovy.control.CompilationFailedException;
import tigase.http.ServiceImpl;
import tigase.http.api.Service;
import tigase.http.util.CSSHelper;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;
import tigase.xmpp.StanzaType;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
public class Servlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(Servlet.class.getCanonicalName());
	
	public static final String MODULE_ID_KEY = "module-id-key";
	public static final String SCRIPTS_DIR_KEY = "scripts-dir";

	private final GStringTemplateEngine templateEngine = new GStringTemplateEngine();

	private Template template = null;
	private Service service = null;
	private File scriptsDir = null;
	
    @Override
    public void init() throws ServletException {
        super.init();
		ServletConfig cfg = super.getServletConfig();
		String moduleName = cfg.getInitParameter(MODULE_ID_KEY);
		service = new ServiceImpl(moduleName);
		scriptsDir = new File(cfg.getInitParameter(SCRIPTS_DIR_KEY));
	}
	
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            processRequest(request, response);
        }
        catch (Exception ex) {
            log.log(Level.FINE, "exception processing request", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
	
    private void processRequest(final HttpServletRequest request, final HttpServletResponse response) throws TigaseStringprepException, IOException, ServletException {
		if (request.getUserPrincipal() == null && !request.authenticate(response)){
			// user not authenticated but authentication is required for this servlet
			return;
		}
		
		final AsyncContext asyncCtx = request.startAsync(request, response);
		
		final Map model = new HashMap();
		retrieveComponentsCommands(request.getUserPrincipal(), (List<Map> commands) -> {
		    model.put("commands", commands);
			
			try {
				String node = request.getParameter("_node");
				String jidStr = request.getParameter("_jid");
				if (node != null && jidStr != null) {
					JID jid = JID.jidInstance(jidStr);
					processRequestStep(request, asyncCtx, model, jid, node, null);
//					executeAdhocForm(request.getUserPrincipal(), jid, node, null, (Command.DataType formType, List<Element> formFields) -> {
//						if (formType == Command.DataType.result || request.getContentLength() == 0) {
//							model.put("formFields", formFields);
//							try {
//								generateResult(asyncCtx, model);
//							} catch (Exception ex) {
//								log.log(Level.SEVERE, "exception processing HTTP request", ex);
//							}
//						} else {
//							setFieldValuesFromRequest(formFields, request);
//							
//							try {
//								executeAdhocForm(request.getUserPrincipal(), jid, node, formFields, (Command.DataType formType1, List<Element> formFields1) -> {
//									if (formType1 == Command.DataType.form && requestHasValuesForFields(formFields1, request)) {
//										setFieldValuesFromRequest(formFields1, request);
//										try {
//											executeAdhocForm(request.getUserPrincipal(), jid, node, formFields1, (Command.DataType formType2, List<Element> formFields2) -> {
//
//												model.put("formFields", formFields2);
//												try {
//													generateResult(asyncCtx, model);
//												} catch (Exception ex) {
//													log.log(Level.SEVERE, "exception processing HTTP request", ex);
//												}
//											});
//										} catch (TigaseStringprepException ex) {
//											log.log(Level.SEVERE, "exception processing HTTP request", ex);
//										}
//									} else {
//										model.put("formFields", formFields1);
//										try {
//											generateResult(asyncCtx, model);
//										} catch (Exception ex) {
//											log.log(Level.SEVERE, "exception processing HTTP request", ex);
//										}
//									}
//								});
//							} catch (TigaseStringprepException ex) {
//								log.log(Level.SEVERE, "exception processing HTTP request", ex);
//							}
//						}
//					});
				} else {
					generateResult(asyncCtx, model);
				}
			} catch (Exception ex) {
				log.log(Level.FINE, "exception processing HTTP request", ex);
			}
		});
	}	
	
	public void processRequestStep(final HttpServletRequest request, final AsyncContext asyncCtx, final Map model, final JID jid, final String node, final List<Element> formFields) throws TigaseStringprepException {
		executeAdhocForm(request.getUserPrincipal(), jid, node, formFields, (Command.DataType formType1, List<Element> formFields1) -> {
			int iteration = model.containsKey("iteration") ? (Integer) model.get("iteration") : 1;
			if (formType1 == Command.DataType.form && ((requestHasValuesForFields(formFields1, request) && (iteration < 10)) || (iteration == 1 && "POST".equals(request.getMethod())))) {
				setFieldValuesFromRequest(formFields1, request, iteration);
				model.put("iteration", ++iteration);
				try {
					processRequestStep(request, asyncCtx, model, jid, node, formFields1);
				} catch (TigaseStringprepException ex) {
					log.log(Level.FINE, "exception processing HTTP request", ex);
				}
			} else {
				model.put("formFields", formFields1);
				try {
					generateResult(asyncCtx, model);
				} catch (Exception ex) {
					log.log(Level.FINE, "exception processing HTTP request", ex);
				}
			}
		});
		
	}
	
	private void generateResult(final AsyncContext asyncCtx, final Map model) throws IOException, CompilationFailedException, ClassNotFoundException {
		Map context = new HashMap();
		context.put("model", model);
		context.put("request", asyncCtx.getRequest());
		context.put("response", asyncCtx.getResponse());

		Map<String, Object> util = new HashMap<>();
		Function<String, String> tmp = (path) -> {
			String content = null;
			try {
				content = CSSHelper.getCssFileContent(path);
			} catch (Exception ex) {}
			if (content == null)
				return "";
			return "<style>" + content + "</style>";
		};
		util.put("inlineCss", tmp);
		context.put("util", util);

		loadTemplate();
		Writable result = template.make(context);
		result.writeTo(asyncCtx.getResponse().getWriter());
		asyncCtx.complete();
	}
	
	private static final String DISCO_ITEMS_XMLNS = "http://jabber.org/protocol/disco#items";
	private static final String ADHOC_COMMANDS_XMLNS = "http://jabber.org/protocol/commands";
	
	
	private void executeAdhocForm(final Principal principal, JID componentJid, String node, List<Element> formFields, final CallbackExecuteForm<List<Element>> callback) throws TigaseStringprepException {
		Element iqEl = new Element("iq");
		iqEl.setXMLNS("jabber:client");
		iqEl.setAttribute("from", principal.getName());
		iqEl.setAttribute("type", StanzaType.set.name());
		iqEl.setAttribute("to", componentJid.toString());
		
		Element commandEl = new Element("command");
		commandEl.setXMLNS(ADHOC_COMMANDS_XMLNS);
		commandEl.setAttribute("node", node);
		iqEl.addChild(commandEl);
		
		if (formFields != null) {
			Element x = new Element("x", new String[] { "xmlns", "type" }, new String[] { "jabber:x:data", "submit" });
			formFields.forEach((Element formField) -> x.addChild(formField));
			commandEl.addChild(x);
		}
		
		Packet iq = Packet.packetInstance(iqEl);
		service.sendPacket(iq, null, (Packet result) -> {
			Element xEl = result.getElement().findChildStaticStr(new String[] { "iq", "command", "x"});
			List<Element> fields = xEl == null ? new ArrayList<>() : xEl.getChildren();
			final Command.DataType formType = (xEl != null && xEl.getAttributeStaticStr("type") != null)
					? Command.DataType.valueOf(xEl.getAttributeStaticStr("type")) : Command.DataType.result;
			fields.forEach((Element e) -> {
				if (e.getName() != "field")
					return;
				if (e.getAttributeStaticStr("type") == null) {
					e.setAttribute("type", formType == Command.DataType.form ? "text-single" : "fixed");
				}
			});
			if (fields.isEmpty()) {
				fields.add(new Element("title", "Execution completed"));
			}
			callback.call(formType, fields);
		});
	}
	
	private void retrieveComponentsCommands(final Principal principal, final Callback<List<Map>> callback) throws TigaseStringprepException {
		retrieveComponents(principal, (List<JID> componentJids) -> {
			final AtomicInteger counter = new AtomicInteger(componentJids.size());
			final List<Map> commands = new ArrayList();
			componentJids.forEach((JID jid) -> {
				try {
					retrieveComponentCommands(principal, jid, (List<Map> componentCommands) -> {
						synchronized (commands) {
							if (componentCommands != null)
								commands.addAll(componentCommands);
						}
						if (counter.decrementAndGet() == 0) {
							callback.call(commands);
						}
					});	} catch (TigaseStringprepException ex) {
					Logger.getLogger(Servlet.class.getName()).log(Level.FINE, null, ex);
				}
			});
		});		
	}
	
	private void retrieveComponents(Principal principal, Callback<List<JID>> callback) throws TigaseStringprepException {
		long start = System.currentTimeMillis();
		Element iqEl = new Element("iq");
		iqEl.setXMLNS("jabber:client");
		iqEl.setAttribute("from", principal.getName());
		BareJID jid = BareJID.bareJIDInstance(principal.getName());
		iqEl.setAttribute("type", StanzaType.get.name());
		iqEl.setAttribute("to", jid.getDomain());
		
		Element queryEl = new Element("query");
		queryEl.setXMLNS(DISCO_ITEMS_XMLNS);
		iqEl.addChild(queryEl);
		
		Packet iq = Packet.packetInstance(iqEl);
		
		service.sendPacket(iq, 1L, (Packet result) -> {
			if (log.isLoggable(Level.FINEST))
				log.log(Level.FINEST, "discovery of components took {0}ms", (System.currentTimeMillis() - start));
			if (result == null || result.getType() != StanzaType.result) {
				log.fine("discovery of components failed");
				callback.call(null);
				return;
			}
			
			List<JID> jids = result.getElement().getChild("query", DISCO_ITEMS_XMLNS).mapChildren((Element item) -> JID.jidInstanceNS(item.getAttributeStaticStr("jid")));
			callback.call(jids);
		});
	}
	
	private void retrieveComponentCommands(Principal principal, JID componentJid, Callback<List<Map>> callback) throws TigaseStringprepException {
		long start = System.currentTimeMillis();
		Element iqEl = new Element("iq");
		iqEl.setXMLNS("jabber:client");
		iqEl.setAttribute("from", principal.getName());
		iqEl.setAttribute("type", StanzaType.get.name());
		iqEl.setAttribute("to", componentJid.toString());
		
		Element queryEl = new Element("query");
		queryEl.setXMLNS(DISCO_ITEMS_XMLNS);
		queryEl.setAttribute("node", ADHOC_COMMANDS_XMLNS);
		iqEl.addChild(queryEl);
		
		Packet iq = Packet.packetInstance(iqEl);
		
		service.sendPacket(iq, 1L, (Packet result) -> {
			if (log.isLoggable(Level.FINEST))
				log.log(Level.FINEST, "discovery of commands of component {0} took {1}ms", new Object[]{componentJid, System.currentTimeMillis() - start});
			if (result == null || result.getType() != StanzaType.result) {
				log.log(Level.FINE, "discovery of component {0} adhoc commands failed", componentJid);
				callback.call(null);
				return;
			}
			
			List<Map> commands = result.getElement().getChild("query", DISCO_ITEMS_XMLNS).mapChildren((Element item) -> item.getAttributes());
			callback.call(commands);
		});		
	}

	private boolean requestHasValuesForFields(List<Element> formFields, HttpServletRequest request) {
		int contains = 0;
		int needed = 0;
		List<String> missing = log.isLoggable(Level.FINEST) ? new ArrayList<>() : null;
		if (formFields != null) {
			for (Element formField : formFields) {
				if (formField.getName() != "field")
					continue;
				
				String type = formField.getAttributeStaticStr("type");
				if (type == null || "boolean".equals(type) || "fixed".equals(type)) {
					continue;
				}

				if (request.getParameter(formField.getAttributeStaticStr("var")) != null
						|| request.getParameterValues(formField.getAttributeStaticStr("var")) != null) {
					contains++;
				} else if (missing != null) {
					missing.add(formField.getAttributeStaticStr("var"));
				}
				needed++;
			}
		}
	
		if (log.isLoggable(Level.FINEST) && contains != needed && needed > 0) {
			log.log(Level.FINEST, "for URI = {0} needed field {1} but got {2}, still missing = {3}",
					new Object[]{ request.getRequestURI() + "?" + request.getQueryString(), needed, contains, missing });
		}
		return contains == needed && needed > 0;
	}
	
	private void setFieldValuesFromRequest(List<Element> formFields, HttpServletRequest request, int iteration) {
		if (formFields == null)
			return;
		
		formFields.forEach((Element formField) -> {
			if (formField.getName() != "field")
				return;
			
			String type = formField.getAttributeStaticStr("type");
			if (type == null)
				return;

			List<Element> orginalChildren = new ArrayList<>();
			if (formField.getChildren() != null) {
				formField.getChildren().forEach((Element oldChild) -> { 
					if (oldChild != null) { 
						formField.removeChild(oldChild);
						orginalChildren.add(oldChild);
					}
				});
			}
			
			String paramName = formField.getAttributeStaticStr("var");
			String value = null;
			switch(type) {
				case "text-multi":
				case "jid-multi":
				case "list-multi":
					String[] values = request.getParameterValues(paramName);
					if (values != null) {
						if (values.length == 1) {
							values = values[0].replace("\r", "").split("\n");
						}
						for (int i=0; i<values.length; i++) {
							value = values[i];
							if (value != null) {
								value = XMLUtils.escape(value);
								formField.addChild(new Element("value", value));
							}
						}
					}
					break;
				case "boolean":
					value = request.getParameter(paramName);
					if (value != null) {
						if ("on".equals(value)) {
							value = "true";
						} else if ("off".equals(value)) {
							value = "false";
						}
						value = XMLUtils.escape(value);
						formField.addChild(new Element("value", value));
					} else {
						formField.addChild(new Element("value", "false"));
					}
					break;
				case "hidden":
					formField.addChildren(orginalChildren);
					break;
				case "list-single":
				case "text-single":
				case "jid-single":
				default:
					value = request.getParameter(paramName);
					if (value != null) {
						value = XMLUtils.escape(value);
						formField.addChild(new Element("value", value));
					}
					break;
			}
		});
	}

	private void loadTemplate() throws IOException, ClassNotFoundException {
		String path = "tigase/admin/index.html";
		File indexFile = new File(path);
		if (indexFile.exists()) {
			template = templateEngine.createTemplate(indexFile);
		} else {
			InputStream is = getClass().getResourceAsStream("/" + path);
			template = templateEngine.createTemplate(new InputStreamReader(is));
		}
	}
	
	private interface Callback<T> { 
		
		public void call(T result);
		
	}
	
	private interface CallbackExecuteForm<T> {
		public void call(Command.DataType formType, T result);
	}
}

