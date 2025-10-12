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
package tigase.http.modules.dashboard;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusEvent;
import tigase.eventbus.HandleEvent;
import tigase.http.api.HttpException;
import tigase.http.jaxrs.Model;
import tigase.http.jaxrs.Page;
import tigase.http.jaxrs.Pageable;
import tigase.http.jaxrs.SecurityContextHolder;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Packet;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostItemImpl;
import tigase.vhosts.VHostManager;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Bean(name = "domains", parent = DashboardModule.class, active = true)
@Path("/domains")
public class DomainsHandler
		extends DashboardHandler
		implements Initializable, UnregisterAware {

	@Inject
	private PermissionsHelper permissionsHelper;
	@Inject
	private UserRepository userRepository;
	@Inject
	private VHostManager vHostManager;
	@Inject
	private EventBus eventBus;
	
	@Override
	public Role getRequiredRole() {
		return Role.User;
	}

	@GET
	@Path("")
	@RolesAllowed({"admin", "user"})
	public Response index(@QueryParam("query") String query, SecurityContext securityContext, Pageable pageable, Model model) {
		List<DomainItem> domains = permissionsHelper.getManagedDomains(securityContext)
				.stream()
				.filter(domain -> query == null || domain.contains(query))
				.skip(pageable.offset()).limit(pageable.pageSize())
				.map(domain -> {
					long noOfUsers = userRepository.getUsersCount(domain);
					VHostItem item = vHostManager.getVHostItem(domain);
					return new DomainItem(domain, item, noOfUsers);
				})
				.sorted(Comparator.comparing(DomainItem::domain))
				.toList();
		model.put("query", query);
		model.put("domains", new Page<>(pageable, domains.size(), domains));
		model.put("canCreateDomain", permissionsHelper.canCreateDomain());
		String output = renderTemplate("domains/index.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	@POST
	@Path("/create")
	@RolesAllowed({"admin", "user"})
	public Response create(@FormParam("domain") String domain, @FormParam("registrationEnabled") boolean registrationEnabled, UriInfo uriInfo) {
		if (!permissionsHelper.canCreateDomain()) {
			throw new HttpException("Forbidden", 403);
		}
		if (vHostManager.getVHostItem(domain) != null) {
			throw new HttpException("Domain already exist!", 409);
		}

		Packet iq = prepareVhostUpdatePacket(domain, command ->{
			Element x = command.getChild("x", "jabber:x:data");
			List<Element> toRemove = x.findChildren(el -> el.getName() == "field" &&
					(VHostItemImpl.REGISTER_ENABLED_LABEL.equals(el.getAttributeStaticStr("var")) ||
							VHostItemImpl.HOSTNAME_LABEL.equals(el.getAttributeStaticStr("var"))));
			if (toRemove != null) {
				toRemove.forEach(x::removeChild);
			}
			DataForm.addFieldValue(command, VHostItemImpl.HOSTNAME_LABEL, domain);
			DataForm.addCheckBoxField(command, VHostItemImpl.REGISTER_ENABLED_LABEL, registrationEnabled);
		});
		applyChange(domain, DomainChangeEvent.Action.add, iq);

		return redirectToIndex(uriInfo, domain);
	}

	@POST
	@Path("/{domain}/changeStatus")
	@RolesAllowed({"admin", "user"})
	public Response changeStatus(@PathParam("domain") String domain, @FormParam("value") boolean value, SecurityContext securityContext, UriInfo uriInfo) {
		if (!permissionsHelper.canManageDomain(securityContext, domain)) {
			throw new HttpException("Forbidden", 403);
		}
		Packet iq = prepareVhostUpdatePacket(domain, command ->{
			Element x = command.getChild("x", "jabber:x:data");
			Element field = x.findChild(el -> el.getName() == "field" && VHostItemImpl.ENABLED_LABEL.equals(el.getAttributeStaticStr("var")));
			x.removeChild(field);
			DataForm.addCheckBoxField(command, VHostItemImpl.ENABLED_LABEL, value);
		});
		applyChange(domain, DomainChangeEvent.Action.update, iq);
		
		return redirectToIndex(uriInfo);
	}

	@POST
	@Path("/{domain}/delete")
	@RolesAllowed({"admin", "user"})
	public Response delete(@PathParam("domain") String domain, SecurityContext securityContext, UriInfo uriInfo) {
		if (!permissionsHelper.canManageDomain(securityContext, domain)) {
			throw new HttpException("Forbidden", 403);
		}
		
		applyChange(domain, DomainChangeEvent.Action.remove, null);

		return redirectToIndex(uriInfo);
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void beforeUnregister() {
		if (eventBus != null) {
			eventBus.unregisterAll(this);
		}
	}

	private void applyChange(String domain, DomainChangeEvent.Action action, Packet iq) {
		if (iq != null) {
			VHostItem item = vHostManager.getComponentRepository().getItemInstance();
			item.initFromCommand(iq);
			String error = vHostManager.getComponentRepository().validateItem(item);
			if (error != null) {
				throw new RuntimeException(error);
			}
		}

		eventBus.fire(new DomainChangeEvent(domain, action, iq == null ? null : iq.getElement()));
	}

	@HandleEvent(filter = HandleEvent.Type.all)
	private void handleEventDomain(DomainChangeEvent event) {
		try {
			VHostItem oldItem = vHostManager.getVHostItem(event.getDomain());

			switch (event.getAction()) {
				case add -> {
					if (oldItem == null) {
						vHostManager.getComponentRepository().addItem(prepareVhostItem(event));
					}
				}
				case update -> {
					vHostManager.getComponentRepository().addItem(prepareVhostItem(event));
				}
				case remove -> {
					vHostManager.getComponentRepository().removeItem(event.domain);
				}
			}
		} catch (TigaseDBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected String renderTemplate(String templateFile, Map<String, Object> model) {
		model.put("permissionsHelper", permissionsHelper);
		return super.renderTemplate(templateFile, model);
	}

	protected Packet prepareVhostUpdatePacket(String domain, Consumer<Element> commandConsumer) {
		VHostItem item = vHostManager.getComponentRepository().getItem(domain);
		if (item == null) {
			item = vHostManager.getComponentRepository().getItemInstance();
		}
		Element command = new Element(Command.COMMAND_EL);
		command.setXMLNS(Command.XMLNS);
		Element iqEl = new Element("iq");
		iqEl.addChild(command);
		Packet iq = Packet.packetInstance(iqEl, null, JID.jidInstanceNS(
				SecurityContextHolder.getSecurityContext().getUserPrincipal().getName()));
		item.addCommandFields(iq);
		commandConsumer.accept(command);
		return iq;
	}

	protected VHostItem prepareVhostItem(DomainChangeEvent event) {
		if (event.getPacket() == null) {
			return null;
		}
		VHostItem item = vHostManager.getComponentRepository().getItemInstance();
		Packet iq = Packet.packetInstance(event.getPacket(), null, null);
		item.initFromCommand(iq);
		return item;
	}

	public static class DomainChangeEvent
			implements EventBusEvent, Serializable {
		public enum Action {
			add,
			update,
			remove
		}

		private String domain;
		private Element packet;
		private Action action;

		public DomainChangeEvent(String domain, Action action, Element packet) {
			this.domain = domain;
			this.action = action;
			this.packet = packet;
		}

		public Action getAction() {
			return action;
		}

		public void setAction(Action action) {
			this.action = action;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public Element getPacket() {
			return packet;
		}

		public void setPacket(Element packet) {
			this.packet = packet;
		}
	}

	public static Response redirectToIndex(UriInfo uriInfo) {
		return redirectToIndex(uriInfo, null);
	}

	public static Response redirectToIndex(UriInfo uriInfo, String query) {
		return Response.seeOther(uriInfo.getBaseUriBuilder().path(DomainsHandler.class, "index").replaceQueryParam("query", query).build()).build();
	}

	public record DomainItem(String domain, VHostItem item, long noOfUsers) {
		public boolean isEnabled() {
			return item != null && item.isEnabled();
		}
	}
}
