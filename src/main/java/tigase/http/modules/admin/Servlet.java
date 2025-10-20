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
package tigase.http.modules.admin;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.WriterOutput;
import tigase.http.modules.AbstractBareModule;
import tigase.http.modules.Module;
import tigase.http.modules.admin.form.Form;
import tigase.http.util.TemplateUtils;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author andrzej
 */
public class Servlet
		extends HttpServlet {

	public static final String MODULE_ID_KEY = "module-id-key";
	public static final String SCRIPTS_DIR_KEY = "scripts-dir";
	private static final Logger log = Logger.getLogger(Servlet.class.getCanonicalName());
	private static final String DISCO_ITEMS_XMLNS = "http://jabber.org/protocol/disco#items";
	private static final String ADHOC_COMMANDS_XMLNS = "http://jabber.org/protocol/commands";
	private File scriptsDir = null;
	private Module module = null;
	private TemplateEngine engine = null;

	@Override
	public void init() throws ServletException {
		super.init();
		ServletConfig cfg = super.getServletConfig();
		String moduleName = cfg.getInitParameter(MODULE_ID_KEY);
		module = AbstractBareModule.getModuleByUUID(moduleName);
		scriptsDir = new File(cfg.getInitParameter(SCRIPTS_DIR_KEY));
		engine =  TemplateUtils.create(null, "tigase.admin", ContentType.Html);
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			if (request.getUserPrincipal() == null) {
				response.setHeader("WWW-Authenticate", "Basic realm=\"TigasePlain\"");
				request.authenticate(response);
				// user not authenticated but authentication is required for this servlet
				return;
			}

			final AsyncContext asyncCtx = request.startAsync(request, response);

			CompletableFuture<List<CommandItem>> future = retrieveComponentsCommands(request.getUserPrincipal());

			future.thenCompose(commands -> {
				final Map model = new HashMap();
				model.put("commands", commands.stream().collect(Collectors.groupingBy(CommandItem::getGroup)));
				model.put("commandGroups", commands.stream().map(CommandItem::getGroup).distinct().sorted().collect(Collectors.toList()));
				model.put("defaultCommands", getDefaultCommands(commands));

				model.put("currentGroup", Optional.ofNullable(request.getParameter("_group")).orElse(""));
				CompletableFuture<Map<String,Object>> futureResult = new CompletableFuture<>();
				Optional<CommandItem> command = getCommand(commands, request.getParameter("_jid"), request.getParameter("_node"));

				if (command.isPresent()) {
					model.put("currentCommand", command.get());
					processRequestStep(request, command.get()).thenAccept(result -> {
						model.put("form", new Form(result.getFields()));
//						model.put("formTitle", result.getFields().stream().filter(el -> el.getName() == "title").findFirst().orElse(null));
//						model.put("formInstructions", result.getFields().stream().filter(el -> el.getName() == "instructions").findFirst().orElse(null));
//						model.put("formFields", result.getFields());
						futureResult.complete(model);
					}).exceptionally(ex -> {
						futureResult.completeExceptionally(ex);
						return null;
					});
				} else {
					futureResult.complete(model);
				}
				return futureResult;
			}).thenCompose(context -> {
				try {
					engine.render("index.jte", context, new WriterOutput(asyncCtx.getResponse().getWriter()));
					return CompletableFuture.completedFuture(null);
				} catch (IOException ex) {
					// nothing we can do, ignoring..
					return CompletableFuture.failedFuture(ex);
				}
			}).exceptionally(ex -> {
				log.log(Level.FINE, "exception processing request", ex);
				try {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} catch (Throwable ex1) {
					// ignoring..
				}
				return null;
			}).whenComplete((r,ex) -> asyncCtx.complete());
		} catch (Exception ex) {
			log.log(Level.FINE, "exception processing request", ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public CompletableFuture<ExecutionResult> processRequestStep(final HttpServletRequest request,
																 final CommandItem command) {
		return processRequestStep(request, command, 1, null);
	}

	public CompletableFuture<ExecutionResult> processRequestStep(final HttpServletRequest request,
								   final CommandItem command, int iteration, final List<Element> formFields) {
		return executeAdhocForm(request.getUserPrincipal(), command, formFields).thenCompose(result -> {
			if (result.isFormType(Command.DataType.form) && ((requestHasValuesForFields(result.getFields(), request) && (iteration < 10)) ||
					(iteration == 1 && "POST".equals(request.getMethod())))) {
				setFieldValuesFromRequest(result.getFields(), request, iteration);
				return processRequestStep(request, command, iteration+1, result.getFields());
			} else {
				return CompletableFuture.completedFuture(result);
			}
		});
	}
	
//	private void generateResult(final AsyncContext asyncCtx, final Map model)
//			throws IOException, CompilationFailedException, ClassNotFoundException {
//		Map context = new HashMap();
//		context.put("model", model);
//		context.put("request", asyncCtx.getRequest());
//		context.put("response", asyncCtx.getResponse());
//
//		Map<String, Object> util = new HashMap<>();
//		Function<String, String> tmp = (path) -> {
//			String content = null;
//			try {
//				content = CSSHelper.getCssFileContent(path);
//			} catch (Exception ex) {
//			}
//			if (content == null) {
//				return "";
//			}
//			return "<style>" + content + "</style>";
//		};
//		util.put("inlineCss", tmp);
//		context.put("util", util);
//
//		engine.render("tigase/admin/index.html", context, new WriterOutput(asyncCtx.getResponse().getWriter()));
//		asyncCtx.complete();
//	}

	static List<CommandItem> getDefaultCommands(List<CommandItem> commands) {
		final List<CommandItem> result = new CopyOnWriteArrayList<>();
		getCommand(commands, "sess-man", "http://jabber.org/protocol/admin#add-user").ifPresent(result::add);
		getCommand(commands, "sess-man", "modify-user").ifPresent(result::add);
		getCommand(commands, "sess-man", "http://jabber.org/protocol/admin#delete-user").ifPresent(result::add);
		getCommand(commands, "sess-man", "http://jabber.org/protocol/admin#get-online-users-list").ifPresent(result::add);
		getCommand(commands, "vhost-man", "comp-repo-item-add").ifPresent(e -> {
			e.setName("Add domain");
			result.add(e);
		});
		getCommand(commands, "vhost-man", "comp-repo-item-update").ifPresent(e -> {
			e.setName("Configure domain");
			result.add(e);
		});
		getCommand(commands, "vhost-man", "comp-repo-item-remove").ifPresent(e -> {
			e.setName("Remove domain");
			result.add(e);
		});
		getCommand(commands, "Rest", "api-key-add").ifPresent(e -> {
			e.setName("Add REST-API key");
			result.add(e);
		});

		return result;
	}
	static Optional<CommandItem> getCommand(List<CommandItem> commands, String component, String command) {
		return commands.stream()
				.filter(map -> map.getNode().equals(command) && map.getJid().toString().startsWith(component))
				.findAny();
	}

	public class ExecutionResult {
		private final Command.DataType formType;
		private final List<Element> fields;

		public ExecutionResult(Command.DataType formType, List<Element> fields) {
			this.formType = formType;
			this.fields = fields;
		}

		public Command.DataType getFormType() {
			return formType;
		}

		public boolean isFormType(Command.DataType formType) {
			return this.formType == formType;
		}

		public List<Element> getFields() {
			return fields;
		}
	}

	private CompletableFuture<ExecutionResult> executeAdhocForm(final Principal principal, CommandItem command, List<Element> formFields) {
		Element iqEl = new Element("iq").withAttribute("xmlns", "jabber:client")
				.withAttribute("type", StanzaType.set.name());

		Element commandEl = new Element("command");
		commandEl.setXMLNS(ADHOC_COMMANDS_XMLNS);
		commandEl.setAttribute("node", command.getNode());
		iqEl.addChild(commandEl);

		if (formFields != null) {
			Element x = new Element("x", new String[]{"xmlns", "type"}, new String[]{"jabber:x:data", "submit"});
			formFields.forEach(x::addChild);
			commandEl.addChild(x);
		}

		Packet iq = Packet.packetInstance(iqEl, JID.jidInstanceNS(principal.getName()), command.getJid());
		CompletableFuture<Packet> future = module.sendPacketAndWait(iq);
		return future.thenApply(result -> {
			Element xEl = result.getElement().findChildStaticStr(new String[]{"iq", "command", "x"});
			List<Element> fields = xEl == null ? null : xEl.getChildren();
			if (fields == null) {
				fields = new ArrayList<>();
			}
			final Command.DataType formType = (xEl != null && xEl.getAttributeStaticStr("type") != null)
											  ? Command.DataType.valueOf(xEl.getAttributeStaticStr("type"))
											  : Command.DataType.result;
			fields.forEach((Element e) -> {
				if (e.getName() != "field") {
					return;
				}
				if (e.getAttributeStaticStr("type") == null) {
					e.setAttribute("type", formType == Command.DataType.form ? "text-single" : "fixed");
				}
			});
			if (fields.isEmpty()) {
				fields.add(new Element("title", "Execution completed"));
			}
			return new ExecutionResult(formType, fields);
		});
	}

	private CompletableFuture<List<CommandItem>> retrieveComponentsCommands(final Principal principal) {
		CompletableFuture<Stream<JID>> allJids = retrieveComponents(principal, JID.jidInstanceNS(BareJID.bareJIDInstanceNS(principal.getName()).getDomain())).thenCombine(
				retrieveComponents(principal, JID.jidInstanceNS(module.getJid().getDomain())),
				(componentJids, httpModuleJids) -> Stream.concat(componentJids.stream(), httpModuleJids.stream()));
		return allJids.thenCompose(componentJids -> {
			CompletableFuture<List<CommandItem>>[] componentFutures = componentJids.map(
					jid -> retrieveComponentCommands(principal, jid)).toArray(CompletableFuture[]::new);
			CompletableFuture<List<CommandItem>> commands = CompletableFuture.allOf(componentFutures)
					.thenApply(x -> Arrays.stream(componentFutures)
							.map(CompletableFuture::join)
							.flatMap(Collection::stream)
							.collect(Collectors.toList()));

			return commands;
		});
	}
	
	private CompletableFuture<List<JID>> retrieveComponents(Principal principal, JID to) {
		long start = System.currentTimeMillis();
		Element iqEl = new Element("iq").withAttribute("xmlns", "jabber:client")
				.withAttribute("type", StanzaType.get.name())
				.withElement("query", DISCO_ITEMS_XMLNS, (String) null);

		Packet iq = Packet.packetInstance(iqEl, JID.jidInstanceNS(principal.getName()), to);

		CompletableFuture<Packet> future = module.sendPacketAndWait(iq, 1).exceptionally(ex -> null);
		return future.thenApply(result -> {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "discovery of components took {0}ms", (System.currentTimeMillis() - start));
			}
			if (result == null || result.getType() != StanzaType.result) {
				log.fine("discovery of components failed");
				return Collections.emptyList();
			}

			return result.getElement()
					.getChild("query", DISCO_ITEMS_XMLNS)
					.mapChildren((Element item) -> JID.jidInstanceNS(item.getAttributeStaticStr("jid")));
		});
	}

	private CompletableFuture<List<CommandItem>> retrieveComponentCommands(Principal principal, JID componentJid) {
		long start = System.currentTimeMillis();
		Element iqEl = new Element("iq").withAttribute("xmlns", "jabber:client")
				.withAttribute("type", StanzaType.get.name())
				.withElement("query", DISCO_ITEMS_XMLNS,
							 queryEl -> queryEl.withAttribute("node", ADHOC_COMMANDS_XMLNS));
		
		Packet iq = Packet.packetInstance(iqEl, JID.jidInstanceNS(principal.getName()), componentJid);

		CompletableFuture<Packet> future = module.sendPacketAndWait(iq, 1).exceptionally(ex -> null);
		return future.thenApply(result -> {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "discovery of commands of component {0} took {1}ms",
						new Object[]{componentJid, System.currentTimeMillis() - start});
			}
			if (result == null || result.getType() != StanzaType.result) {
				log.log(Level.FINE, "discovery of component {0} adhoc commands failed", componentJid);
				return Collections.emptyList();
			}

			return Optional.ofNullable(result.getElement().getChild("query", DISCO_ITEMS_XMLNS))
					.stream()
					.map(Element::getChildren)
					.filter(Objects::nonNull)
					.flatMap(Collection::stream)
					.map(Element::getAttributes)
					.map(CommandItem::new)
					.collect(Collectors.toList());
		});
	}

	private boolean requestHasValuesForFields(List<Element> formFields, HttpServletRequest request) {
		int contains = 0;
		int needed = 0;
		List<String> missing = log.isLoggable(Level.FINEST) ? new ArrayList<>() : null;
		if (formFields != null) {
			for (Element formField : formFields) {
				if (formField.getName() != "field") {
					continue;
				}

				String type = formField.getAttributeStaticStr("type");
				if (type == null || "boolean".equals(type) || "fixed".equals(type)) {
					continue;
				}

				String name = formField.getAttributeStaticStr("var");
				if (request.getParameter(name) != null ||
						request.getParameterValues(name) != null) {
					contains++;
				} else if (name != null && formField.getAttributeStaticStr(name + "_date") != null
						&& formField.getAttributeStaticStr(name + "_tz") != null) {
					contains++;
				} else if (missing != null) {
					missing.add(name);
				}
				needed++;
			}
		}

		if (log.isLoggable(Level.FINEST) && contains != needed && needed > 0) {
			log.log(Level.FINEST, "for URI = {0} needed field {1} but got {2}, still missing = {3}",
					new Object[]{request.getRequestURI() + "?" + request.getQueryString(), needed, contains, missing});
		}
		return contains == needed && needed > 0;
	}

	private void setFieldValuesFromRequest(List<Element> formFields, HttpServletRequest request, int iteration) {
		if (formFields == null) {
			return;
		}

		formFields.forEach((Element formField) -> {
			if (formField.getName() != "field") {
				return;
			}

			String type = formField.getAttributeStaticStr("type");
			if (type == null) {
				return;
			}
			String subtype = formField.getAttributeStaticStr("subtype");

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
			switch (type) {
				case "text-multi":
				case "jid-multi":
				case "list-multi":
					String[] values = request.getParameterValues(paramName);
					if (values != null) {
						if (values.length == 1) {
							values = values[0].replace("\r", "").split("\n");
						}
						for (int i = 0; i < values.length; i++) {
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
				case "fixed":
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
					} else if ("datetime" == subtype) {
						value = Optional.ofNullable(request.getParameter(paramName + "_date"))
								.filter(val -> !val.isEmpty())
								.orElse(null);
						if (value != null) {
							String minutes = Optional.ofNullable(request.getParameter(paramName + "_time"))
									.filter(val -> !val.isEmpty())
									.orElse("00:00:00");
							if (minutes.length() == 5) {
								minutes += ":00";
							}
							TimeZone tz = Optional.ofNullable(request.getParameter(paramName + "_tz"))
									.filter(val -> !val.isEmpty()).map(val -> TimeZone.getTimeZone(val))
									.orElse(TimeZone.getTimeZone("UTC"));
							value += "T" + minutes + "[" + tz.getID() + "]";
							DateTimeFormatter formatter = new DateTimeFormatterBuilder()
									.append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
									.optionalStart()
									.appendLiteral('[')
									.parseCaseSensitive()
									.appendZoneRegionId()
									.appendLiteral(']')
									.toFormatter();

							TemporalAccessor x = formatter.parse(value);
							ZonedDateTime timestamp = LocalDateTime.from(x).atZone(ZoneId.from(x));
							value = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp);
							value = XMLUtils.escape(value);
							formField.addChild(new Element("value", value));
						}
					}
					break;
			}
		});
	}
	
}

