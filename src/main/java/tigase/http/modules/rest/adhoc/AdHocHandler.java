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
package tigase.http.modules.rest.adhoc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import tigase.http.api.HttpException;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.DataForm;
import tigase.server.Iq;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static tigase.component.modules.impl.DiscoveryModule.DISCO_ITEMS_XMLNS;

@Bean(name = "adhoc", parent = RestModule.class, active = true)
@Path("/adhoc")
public class AdHocHandler
		extends AbstractRestHandler {

	private static final String COMMAND_XMLNS = "http://jabber.org/protocol/commands";
	private static final Map<String, DataForm.FieldType> FIELD_TYPE_MAP = Arrays.stream(DataForm.FieldType.values())
			.collect(Collectors.toUnmodifiableMap(DataForm.FieldType::value, Function.identity()));

	@Inject
	private RestModule module;

	public AdHocHandler() {
		super(Security.None, Role.User);
	}

	public RestModule getModule() {
		return module;
	}

	public void setModule(RestModule module) {
		this.module = module;
	}
	
	@GET
	@Path("/{componentJid}")
	@Produces({"application/json", "application/xml"})
	@Operation(summary = "List ad-hoc commands", description = "Get list of available ad-hoc commands")
	@ApiResponse(responseCode = "200", description = "List of ad-hoc commands", content = {@Content(schema = @Schema(implementation = AdHocCommands.class)) })
	public void list(SecurityContext securityContext, @Parameter(description = "Bare JID of the component to query") @NotNull @PathParam("componentJid") BareJID componentJid, @Suspended AsyncResponse asyncResponse)
			throws HttpException {
		tigase.xml.Element iqEl = new tigase.xml.Element("iq").withAttribute("type", StanzaType.get.name())
				.withElement("query", DISCO_ITEMS_XMLNS, queryEl -> {
					queryEl.withAttribute("node", COMMAND_XMLNS);
				});

		Iq iq = new Iq(iqEl, JID.jidInstanceNS(securityContext.getUserPrincipal().getName()), JID.jidInstance(componentJid));
		sendResult(getModule().sendPacketAndWait(iq).thenCompose(result -> {
			tigase.xml.Element queryEl = result.getElemChild("query", DISCO_ITEMS_XMLNS);
			if (queryEl != null) {
				return CompletableFuture.completedFuture(new AdHocCommands(
						Optional.ofNullable(queryEl.findChildren(it -> "item" == it.getName()))
								.stream()
								.flatMap(list -> list.stream())
								.map(item -> new AdHocCommands.Command(
										JID.jidInstanceNS(item.getAttributeStaticStr("jid")),
										item.getAttributeStaticStr("node"), item.getAttributeStaticStr("name")))
								.collect(Collectors.toList())));
			} else {
				return CompletableFuture.failedFuture(new HttpException("Received unexpected response", HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
			}
		}), asyncResponse);
	}

	@POST
	@Path("/{componentJid}/{node}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	@Operation(summary = "Execute ad-hoc command", description = "Execute ad-hoc command")
	@ApiResponse(responseCode = "200", description = "Result of execution of ad-hoc command", content = {@Content(schema = @Schema(implementation = ExecuteResult.class)) })
	public void execute(SecurityContext securityContext,
					 @Parameter(description = "Bare JID of the component") @NotNull @PathParam("componentJid") BareJID componentJid,
					 @Parameter(description = "Id if the command") @NotNull @PathParam("node") String node, ExecuteRequests payload,
					 @Suspended AsyncResponse asyncResponse)
			throws HttpException {
		tigase.xml.Element iqEl = new tigase.xml.Element("iq").withAttribute("type", StanzaType.set.name())
				.withElement("command", COMMAND_XMLNS, commandEl -> {
					commandEl.withAttribute("node", node);

					if (payload != null && !payload.getFields().isEmpty()) {
						commandEl.withElement("x", "jabber:x:data", xEl -> {
							xEl.withAttribute("type", "submit");
							for (ExecuteRequests.Field field : payload.getFields()) {
								xEl.withElement("field", fieldEl -> {
									fieldEl.withAttribute("var", field.getVar());
									for (String value : field.getValue()) {
										fieldEl.withElement("value", null, value);
									}
								});
							}
						});
					}
				});



		Iq iq = new Iq(iqEl, JID.jidInstanceNS(securityContext.getUserPrincipal().getName()), JID.jidInstance(componentJid));
		sendResult(getModule().sendPacketAndWait(iq).thenCompose(result -> {
			Optional<tigase.xml.Element> data = Optional.ofNullable(result.getElemChild("command", COMMAND_XMLNS))
					.map(el -> el.getChild("x", "jabber:x:data"));
			if (data.isEmpty()) {
				return CompletableFuture.failedFuture(new HttpException("Received unexpected response", HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
			}

			ExecuteResult r = new ExecuteResult();
			if (data.get().getChildren() != null) {
				for (tigase.xml.Element child : data.get().getChildren()) {
					switch (child.getName()) {
						case "field" -> {
							if (r.getFields() == null) {
								r.setFields(new ArrayList<>());
							}
							r.getFields().add(ExecuteResult.Field.fromElement(child));
						}
						case "title" -> r.setTitle(XMLUtils.unescape(child.getCData()));
						case "instructions" -> r.setInstructions(XMLUtils.unescape(child.getCData()));
						case "note" -> r.setNote(XMLUtils.unescape(child.getCData()));
						case "reported" -> {
							ExecuteResult.Reported reported = new ExecuteResult.Reported();
							reported.setColumns(child.streamChildren().filter(el -> el.getName() == "field").map(
									ExecuteResult.Field::fromElement).collect(Collectors.toList()));
							if (r.getReported() == null) {
								r.setReported(new ArrayList<>());
							}
							r.getReported().add(reported);
						}
						case "item" -> {
							r.getReported().stream().findFirst().ifPresent(reported -> {
								reported.getRows().add(child.streamChildren().map(it -> {
									ExecuteResult.Reported.Item item = new ExecuteResult.Reported.Item();
									item.setVar(it.getAttributeStaticStr("var"));
									item.setValue(it.streamChildren()
														  .filter(it2 -> it2.getName() == "value")
														  .findFirst()
														  .map(Element::getCData)
														  .map(XMLUtils::unescape)
														  .orElse(null));
									return item;
								}).collect(Collectors.toList()));
							});
						}
						case "query" -> {
							if (child.getXMLNS() == "jabber:x:roster") {
								ExecuteResult.Roster roster = new ExecuteResult.Roster();
								roster.setItems(child.streamChildren().map(el -> {
									ExecuteResult.Roster.Item item = new ExecuteResult.Roster.Item();
									item.setJid(BareJID.bareJIDInstanceNS(el.getAttributeStaticStr("jid")));
									item.setName(el.getAttributeStaticStr("name"));
									Optional.ofNullable(el.getAttributeStaticStr("subscription"))
											.map(RosterAbstract.SubscriptionType::valueOf)
											.ifPresent(item::setSubscription);
									item.setGroups(el.streamChildren()
														   .filter(el1 -> el1.getName() == "group")
														   .map(Element::getCData)
														   .map(XMLUtils::unescape)
														   .collect(Collectors.toList()));
									return item;
								}).collect(Collectors.toList()));
								r.setRoster(roster);
							}
						}
						default -> {}
					}
				}
			}
			return CompletableFuture.completedFuture(r);
		}), asyncResponse);
	}

	@XmlRootElement(name = "execute")
	public static class ExecuteRequests {

		@XmlElement(name = "field")
		private List<Field> fields = new ArrayList<>();

		public List<Field> getFields() {
			return fields;
		}

		public void setFields(List<Field> fields) {
			this.fields = fields;
		}

		public static class Field {

			@XmlAttribute
			@NotNull
			private String var;

			private List<String> value = new ArrayList<>();

			public String getVar() {
				return var;
			}

			public void setVar(String var) {
				this.var = var;
			}

			public List<String> getValue() {
				return value;
			}

			public void setValue(List<String> value) {
				this.value = value;
			}
		}

	}

	@XmlRootElement(name = "result")
	public static class ExecuteResult {

		private String title;
		private String instructions;
		private String note;

		@XmlElement(name = "field")
		private List<Field> fields;

		@XmlElement(name = "reported")
		private List<Reported> reported;

		private Roster roster = null;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getInstructions() {
			return instructions;
		}

		public void setInstructions(String instructions) {
			this.instructions = instructions;
		}

		public String getNote() {
			return note;
		}

		public void setNote(String note) {
			this.note = note;
		}

		public List<Field> getFields() {
			return fields;
		}

		public void setFields(List<Field> fields) {
			this.fields = fields;
		}

		public List<Reported> getReported() {
			return reported;
		}

		public void setReported(List<Reported> reported) {
			this.reported = reported;
		}

		public Roster getRoster() {
			return roster;
		}

		public void setRoster(Roster roster) {
			this.roster = roster;
		}

		public static class Field {

			public static Field fromElement(Element child) {
				Field field = new ExecuteResult.Field();
				field.setVar(child.getAttributeStaticStr("var"));
				field.setLabel(child.getAttributeStaticStr("label"));
				field.setValue(child.streamChildren()
									   .filter(el -> el.getName() == "value")
									   .map(Element::getCData)
									   .map(XMLUtils::unescape)
									   .collect(Collectors.toList()));
				field.setType(FIELD_TYPE_MAP.get(child.getAttributeStaticStr("type")));
				if (field.getType() != null) {
					switch (field.getType()) {
						case ListSingle, ListMulti -> {
							field.setOptions(child.streamChildren()
													 .filter(el -> el.getName() == "option")
													 .map(ExecuteResult.Field.Option::fromElement)
													 .collect(Collectors.toList()));
						}
						default -> {}
					}
				}
				return field;
			}

			@XmlAttribute
			@NotNull
			private String var;
			@XmlAttribute
			private DataForm.FieldType type;
			@XmlAttribute
			private String label;
			@XmlElement(name = "option")
			private List<Option> options;
			private List<String> value;

			public String getVar() {
				return var;
			}

			public void setVar(String var) {
				this.var = var;
			}

			public String getLabel() {
				return label;
			}

			public void setLabel(String label) {
				this.label = label;
			}

			public DataForm.FieldType getType() {
				return type;
			}

			public void setType(DataForm.FieldType type) {
				this.type = type;
			}

			public List<String> getValue() {
				return value;
			}

			public void setValue(List<String> value) {
				this.value = value;
			}

			public List<Option> getOptions() {
				return options;
			}

			public void setOptions(List<Option> options) {
				this.options = options;
			}

			public static class Option {

				public static Option fromElement(Element el) {
					Option option = new Option();
					option.setLabel(el.getAttributeStaticStr("label"));
					option.setValue(XMLUtils.unescape(el.getCData()));
					return option;
				}

				@XmlAttribute
				private String label;
				@XmlValue
				@NotNull
				private String value;

				public String getLabel() {
					return label;
				}

				public void setLabel(String label) {
					this.label = label;
				}

				public String getValue() {
					return value;
				}

				public void setValue(String value) {
					this.value = value;
				}
			}
		}

		public static class Reported {

			private List<Field> columns = new ArrayList<>();

			private List<List<Item>> rows = new ArrayList<>();

			public List<Field> getColumns() {
				return columns;
			}

			public void setColumns(List<Field> columns) {
				this.columns = columns;
			}

			public List<List<Item>> getRows() {
				return rows;
			}

			public void setRows(List<List<Item>> rows) {
				this.rows = rows;
			}

			public static class Item {
				private String var;
				private String value;

				public String getVar() {
					return var;
				}

				public void setVar(String var) {
					this.var = var;
				}

				public String getValue() {
					return value;
				}

				public void setValue(String value) {
					this.value = value;
				}
			}
		}

		@XmlRootElement(name = "roster")
		public static class Roster {

			@XmlElement(name = "item")
			private List<Item> items = new ArrayList<>();

			public List<Item> getItems() {
				return items;
			}

			public void setItems(List<Item> items) {
				this.items = items;
			}

			public static class Item {
				@XmlAttribute
				@NotNull
				private BareJID jid;
				@XmlAttribute
				private String name;
				@XmlAttribute
				private RosterAbstract.SubscriptionType subscription;
				@XmlElement(name = "group")
				private List<String> groups = new ArrayList<>();

				public BareJID getJid() {
					return jid;
				}

				public void setJid(BareJID jid) {
					this.jid = jid;
				}

				public String getName() {
					return name;
				}

				public void setName(String name) {
					this.name = name;
				}

				public RosterAbstract.SubscriptionType getSubscription() {
					return subscription;
				}

				public void setSubscription(RosterAbstract.SubscriptionType subscription) {
					this.subscription = subscription;
				}

				public List<String> getGroups() {
					return groups;
				}

				public void setGroups(List<String> groups) {
					this.groups = groups;
				}
			}
		}
	}

	@XmlRootElement(name = "commands")
	public static class AdHocCommands {

		@XmlElement(name = "command")
		private List<Command> commands = new ArrayList<>();

		public AdHocCommands() {
		}

		public AdHocCommands(List<Command> commands) {
			this.commands = commands;
		}

		public List<Command> getCommands() {
			return commands;
		}

		public void setCommands(List<Command> commands) {
			this.commands = commands;
		}

		public static class Command {
			@XmlAttribute
			@NotNull
			private String node;
			@XmlAttribute
			@NotNull
			private JID jid;
			@XmlAttribute
			private String name;

			public Command() {}

			public Command(JID jid, String node, String name) {
				this.jid = jid;
				this.node = node;
				this.name = name;
			}

			public String getNode() {
				return node;
			}

			public void setNode(String node) {
				this.node = node;
			}

			public JID getJid() {
				return jid;
			}

			public void setJid(JID jid) {
				this.jid = jid;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}
		}
	}

}
