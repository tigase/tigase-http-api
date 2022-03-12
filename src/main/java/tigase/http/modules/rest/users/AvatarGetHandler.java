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
package tigase.http.modules.rest.users;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;
import tigase.http.ServiceImpl;
import tigase.http.api.NotFoundException;
import tigase.http.api.Service;
import tigase.http.modules.rest.AbstractRestHandler;
import tigase.http.modules.rest.RestModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Iq;
import tigase.util.Base64;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Bean(name = "avatarGet", parent = RestModule.class, active = false)
@Path("/avatar")
public class AvatarGetHandler extends AbstractRestHandler {

	private static final Pattern DATA_PATTERN = Pattern.compile("data:(.+);base64,(.+)");
	private static final String[] PEP_DATA_PATH = { Iq.ELEM_NAME, "pubsub", "items", "item", "data" };
	private static final String[] PEP_METADATA_PATH = { Iq.ELEM_NAME, "pubsub", "items", "item", "metadata" };
	private static final String[] VCARD_TEMP_PHOTO_PATH = { Iq.ELEM_NAME, "vCard", "PHOTO" };
	private static final String[] VCARD4_PHOTO_URI_PATH = { Iq.ELEM_NAME, "vCard", "PHOTO" };

	@Inject
	private RestModule restModule;
	private Service<RestModule> service;

	public AvatarGetHandler() {
		super(Security.None, Role.None);
	}

	public RestModule getRestModule() {
		return restModule;
	}

	public void setRestModule(RestModule restModule) {
		this.restModule = restModule;
		service = (Service<RestModule>) new ServiceImpl<RestModule>(restModule);
	}

	@GET
	@Path("/{user}(/{source})?")
	public void retrieveAvatar(@NotNull @PathParam("user") String userStr, @PathParam("source") Source source, @Suspended AsyncResponse asyncResponse) {
		BareJID user = BareJID.bareJIDInstanceNS(userStr);
		
		getAvatar(user, source).thenAccept(asyncResponse::resume).exceptionally(ex -> {
			asyncResponse.resume(ex);
			return null;
		});
	}

	public CompletableFuture<Response> getAvatar(BareJID userJid, Source source) {
		if (source != null) {
			return switch (source) {
				case pep -> getAvatarFromPEP(userJid);
				case vcard4 -> getAvatarFromVCard4(userJid);
				case vcardTemp -> getAvatarFromVCardTemp(userJid);
			};
		} else {
			return getAvatarFromPEP(userJid).exceptionallyCompose(ex1 -> getAvatarFromVCard4(userJid))
					.exceptionallyCompose(ex2 -> getAvatarFromVCardTemp(userJid));
		}
	}

	public CompletableFuture<Response> getAvatarFromPEP(BareJID userJid) {
		Element iqEl = new Element("iq").withAttribute("type", "get")
				.withElement("pubsub", "http://jabber.org/protocol/pubsub", pubsubEl -> {
					pubsubEl.withElement("items", itemsEl -> {
						itemsEl.withAttribute("node", "urn:xmpp:avatar:metadata").withAttribute("max_items", "1");
					});
				});

		Iq iq = new Iq(iqEl, null, JID.jidInstance(userJid));

		return service.sendPacketAndWait(iq, 30).thenCompose(result -> {
			Optional<Element> infos = Optional.ofNullable(result.getElement().getChildren(PEP_METADATA_PATH))
					.flatMap(el -> el.stream().filter(it -> "info".equals(it.getName())).findFirst());
			if (infos.isPresent()) {
				Optional<URI> uri = infos.map(info -> info.getAttributeStaticStr("url")).map(urlStr -> {
					try {
						return new URI(urlStr);
					} catch (Throwable ex) {
					}
					return null;
				});

				if (uri.isPresent()) {
					return CompletableFuture.completedFuture(Response.temporaryRedirect(uri.get()).build());
				}

				Optional<String> id = infos.map(it -> it.getAttributeStaticStr("id"));
				Optional<String> mimeType = infos.map(it -> it.getAttributeStaticStr("type"));
				if (id.isPresent() && mimeType.isPresent()) {
					return getAvatarDataFromPEP(userJid, id.get(), mimeType.get());
				}
			}

			return CompletableFuture.failedFuture(new NotFoundException());
		});
	}

	public CompletableFuture<Response> getAvatarDataFromPEP(BareJID userJid, String id, String mimeType) {
		Element iqEl = new Element("iq").withAttribute("type", "get")
				.withElement("pubsub", "http://jabber.org/protocol/pubsub", pubsubEl -> {
					pubsubEl.withElement("items", itemsEl -> {
						itemsEl.withAttribute("node", "urn:xmpp:avatar:data")
								.withElement("item", itemEl -> itemEl.withAttribute("id", id));
					});
				});

		Iq iq = new Iq(iqEl, null, JID.jidInstance(userJid));

		return service.sendPacketAndWait(iq, 30).thenCompose(result -> {
			Optional<byte[]> data = Optional.ofNullable(result.getElement().getChildCData(PEP_DATA_PATH))
					.map(XMLUtils::unescape)
					.map(str -> str.replace("\n", "").replace("\r", ""))
					.map(Base64::decode);
			if (data.isPresent()) {
				return CompletableFuture.completedFuture(Response.ok(data.get(), mimeType).build());
			} else {
				return CompletableFuture.failedFuture(new NotFoundException());
			}
		});
	}

	public CompletableFuture<Response> getAvatarFromVCard4(BareJID userJid) {
		Element iqEl = new Element("iq")
				.withAttribute("type", "get")
				.withElement("vcard", "urn:ietf:params:xml:ns:vcard-4.0", (String) null);

		Iq iq = new Iq(iqEl, null, JID.jidInstance(userJid));

		return service.sendPacketAndWait(iq, 30).thenCompose(result -> {
			Optional<String> photoUri = Optional.ofNullable(result.getElement().getCData(VCARD4_PHOTO_URI_PATH))
					.map(XMLUtils::unescape)
					.map(str -> str.replace("\n", "").replace("\r", ""));
			if (photoUri.isPresent()) {
				Matcher matcher = DATA_PATTERN.matcher(photoUri.get());
				if (matcher.matches()) {
					String mimetype = matcher.group(0);
					String data = matcher.group(1);
					return CompletableFuture.completedFuture(Response.ok(Base64.decode(data), mimetype).build());
				} else {
					try {
						URI url = new URI(photoUri.get());
						return CompletableFuture.completedFuture(Response.temporaryRedirect(url).build());
					} catch (Throwable ex) {
					}
				}
			}

			return CompletableFuture.failedFuture(new NotFoundException());
		});
	}

	public CompletableFuture<Response> getAvatarFromVCardTemp(BareJID userJid) {
		Element iqEl = new Element("iq")
				.withAttribute("type", "get")
				.withElement("vCard", "vcard-temp", (String) null);

		Iq iq = new Iq(iqEl, null, JID.jidInstance(userJid));

		return service.sendPacketAndWait(iq, 30).thenCompose(result -> {
			Element photoEl = result.getElement().findChild(VCARD_TEMP_PHOTO_PATH);
			if (photoEl != null) {
				Optional<java.net.URI> url = Optional.ofNullable(photoEl.getChild("EXTVAL"))
						.map(Element::getCData)
						.map(XMLUtils::unescape)
						.map(urlStr -> {
							try {
								return new URI(urlStr);
							} catch (Throwable ex) {
								return null;
							}
						});
				if (url.isPresent()) {
					return CompletableFuture.completedFuture(Response.temporaryRedirect(url.get()).build());
				}

				Optional<String> mimeType = Optional.ofNullable(photoEl.getChild("TYPE"))
						.map(Element::getCData)
						.map(XMLUtils::unescape);
				Optional<byte[]> data = Optional.ofNullable(photoEl.getChild("BINVAL"))
						.map(Element::getCData)
						.map(str -> str.replace("\n", "").replace("\r", ""))
						.map(Base64::decode);

				if (mimeType.isPresent() && data.isPresent()) {
					return CompletableFuture.completedFuture(Response.ok(data.get(), mimeType.get()).build());
				}
			}

			return CompletableFuture.failedFuture(new NotFoundException());
		});
	}

	public enum Source {
		pep,
		vcard4,
		vcardTemp;

		public String getValue() {
			return switch (this) {
				case pep -> "pep";
				case vcard4 -> "vcard4";
				case vcardTemp -> "vcard-temp";
			};
		}

		private static final Map<String,Source> MAPPING = Arrays.stream(Source.values()).collect(
				Collectors.toUnmodifiableMap(Source::getValue, Function.identity()));

		public static Source fromString(String str) {
			return MAPPING.get(str);
		}
	}

}
