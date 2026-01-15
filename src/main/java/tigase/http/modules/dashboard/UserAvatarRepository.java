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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.Base64;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Bean(name = "userAvatarRepository", parent = DashboardModule.class, active = true)
public class UserAvatarRepository {

	private static final String[] PEP_DATA_PATH = { Iq.ELEM_NAME, "pubsub", "items", "item", "data" };
	private static final String[] PEP_METADATA_PATH = { Iq.ELEM_NAME, "pubsub", "items", "item", "metadata" };

	@Inject
	private DashboardModule module;

	public CompletableFuture<byte[]> getData(@NonNull BareJID userJid, @NonNull String id) {
		Element iqEl = new Element("iq").withAttribute("type", "get")
				.withElement("pubsub", "http://jabber.org/protocol/pubsub", pubsubEl -> {
					pubsubEl.withElement("items", itemsEl -> {
						itemsEl.withAttribute("node", "urn:xmpp:avatar:data")
								.withElement("item", itemEl -> itemEl.withAttribute("id", id));
					});
				});

		Iq iq = new Iq(iqEl, null, JID.jidInstance(userJid));

		return sendPacketAndWait(iq, 30).thenApply(result -> {
			return Optional.ofNullable(result.getElement().getChildCData(PEP_DATA_PATH))
					.map(XMLUtils::unescape)
					.map(str -> str.replace("\n", "").replace("\r", ""))
					.map(Base64::decode).orElse(null);
		});
	}

	public CompletableFuture<AvatarMetadata> getMetadata(@NonNull BareJID userJid) {
		Element iqEl = new Element("iq").withAttribute("type", "get")
				.withElement("pubsub", "http://jabber.org/protocol/pubsub", pubsubEl -> {
					pubsubEl.withElement("items", itemsEl -> {
						itemsEl.withAttribute("node", "urn:xmpp:avatar:metadata").withAttribute("max_items", "1");
					});
				});
		Iq iq = new Iq(iqEl, null, JID.jidInstance(userJid));
		return sendPacketAndWait(iq, 10).thenApply(result -> {
			Element metadata = result.getElement().findChild(PEP_METADATA_PATH);
			return AvatarMetadata.parse(metadata);
//			Element info = Optional.ofNullable(result.getElement().getChildren(PEP_METADATA_PATH))
//					.flatMap(el -> el.stream().filter(it -> "info".equals(it.getName())).findFirst())
//					.orElse(null);
//			if (info == null) {
//				return null;
//			}
//
//			String url = info.getAttributeStaticStr("url");
//			String bytes = info.getAttributeStaticStr("bytes");
//			String id = info.getAttributeStaticStr("id");
//			String type = info.getAttributeStaticStr("type");
//			if (id != null && type != null) {
//				return new AvatarMetadata(id, type, Long.parseLong(bytes), url);
//			} else {
//				return null;
//			}
		});
	}
	
	public CompletableFuture<Void> setData(@NonNull BareJID userJid, @NonNull String id, byte @NonNull [] data) {
		Element dataEl = new Element("data", Base64.encode(data)).withAttribute("xmlns", "urn:xmpp:avatar:data");
		Element iqEl = new Element("iq").withAttribute("type", "set")
				.withElement("pubsub", "http://jabber.org/protocol/pubsub", pubsubEl -> {
					pubsubEl.withElement("publish", publishEl -> {
						publishEl.withAttribute("node", "urn:xmpp:avatar:data")
								.withElement("item", itemEl -> {
									itemEl.withAttribute("id", id).addChild(dataEl);
								});
					});
				});
		Iq iq = new Iq(iqEl, JID.jidInstance(userJid), JID.jidInstance(userJid));
		return sendPacketAndWait(iq, 10).thenApply(result -> null);
	}

	public CompletableFuture<Void> deleteMetadata(@NotNull BareJID userJid) {
		Element metadataEl = new Element("metadata").withAttribute("xmlns", "urn:xmpp:avatar:metadata");
		return setMetadata(userJid, null, metadataEl);
	}

	public CompletableFuture<Void> setMetadata(@NonNull BareJID userJid, @NonNull AvatarMetadata metadata) {
		Element metadataEl = metadata.toElement();
		return setMetadata(userJid, metadata.id(), metadataEl);
	}

	private CompletableFuture<Void> setMetadata(@NonNull BareJID userJid, @Nullable String id, @NonNull Element metadataEl) {
		Element iqEl = new Element("iq").withAttribute("type", "set")
				.withElement("pubsub", "http://jabber.org/protocol/pubsub", pubsubEl -> {
					pubsubEl.withElement("publish", publishEl -> {
						publishEl.withAttribute("node", "urn:xmpp:avatar:metadata")
								.withElement("item", itemEl -> {
									if (id != null) {
										itemEl.withAttribute("id", id);
									}
									itemEl.addChild(metadataEl);
								});
					});
				});
		Iq iq = new Iq(iqEl, JID.jidInstance(userJid), JID.jidInstance(userJid));
		return sendPacketAndWait(iq, 10).thenApply(result -> null);
	}

	public record AvatarMetadata(@NonNull String id, @NonNull String mimeType, long bytes, String url) {

		public static @Nullable AvatarMetadata parse(@Nullable Element element) {
			if (element == null || !element.matches(el -> Objects.equals("metadata", el.getName()) &&
					Objects.equals("urn:xmpp:avatar:metadata", el.getXMLNS()))) {
				return null;
			}

			Element info = element.getChild("info");
			if (info == null) {
				return null;
			}

			String url = info.getAttributeStaticStr("url");
			String bytes = info.getAttributeStaticStr("bytes");
			String id = info.getAttributeStaticStr("id");
			String type = info.getAttributeStaticStr("type");
			if (id != null && type != null) {
				return new AvatarMetadata(id, type, Long.parseLong(bytes), url);
			} else {
				return null;
			}
		}

		public @NonNull Element toElement() {
			return new Element("metadata").withAttribute("xmlns", "urn:xmpp:avatar:metadata")
				.withElement("info", infoEl -> {
					infoEl.withAttribute("id", id())
							.withAttribute("type", mimeType())
							.withAttribute("bytes", String.valueOf(bytes()));
					Optional.ofNullable(url).ifPresent(url -> infoEl.withAttribute("url", url));
				});
		}

	}

	protected CompletableFuture<Packet> sendPacketAndWait(@NonNull Packet packet, Integer timeout) {
		return module.sendPacketAndWait(packet,timeout).thenCompose(result -> {
			if (result.getType() == StanzaType.error) {
				return CompletableFuture.failedFuture(new RuntimeException(result.getErrorCondition()));
			} else {
				return CompletableFuture.completedFuture(result);
			}
		});
	}
}
