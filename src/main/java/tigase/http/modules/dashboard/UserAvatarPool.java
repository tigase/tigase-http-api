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
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.Base64;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.jid.BareJID;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "userAvatarPool", parent = DashboardModule.class, active = true)
public class UserAvatarPool implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(UserAvatarPool.class.getCanonicalName());
	private static final String SUBNODE = "user-avatar-pool";
	private static final Random random = new SecureRandom();

	@Inject
	private EventBus eventBus;
	@Inject
	private DashboardModule module;
	@Inject
	private UserRepository userRepository;
	@Inject
	private UserAvatarRepository userAvatarRepository;
	@Inject
	private VHostManagerIfc vHostManager;
	@ConfigField(desc = "Enable user avatar pool")
	private boolean enable = false;
	private boolean initialized = false;

	public boolean isEnabled() {
		return enable;
	}

	@Override
	public void beforeUnregister() {
		if (eventBus != null) {
			eventBus.unregisterAll(this);
		}
	}

	@Override
	public void initialize() {
		if (eventBus != null) {
			eventBus.registerAll(this);
		}
	}

	public @NonNull List<@NonNull String> getKeys() throws TigaseDBException {
		initRepo();
		return Optional.ofNullable(userRepository.getKeys(getRepoUserJid(), SUBNODE))
				.map(Arrays::asList)
				.orElseGet(Collections::emptyList);
	}

	public @Nullable Avatar getItem(@NonNull String key) throws TigaseDBException {
		initRepo();
		String data = userRepository.getData(getRepoUserJid(), SUBNODE, key);
		if (data == null) {
			return null;
		}
		DomBuilderHandler domHandler = new DomBuilderHandler();
		SimpleParser parser = SingletonFactory.getParserInstance();
		parser.parse(domHandler, data.toCharArray(), 0, data.length());

		Queue<Element> elems = domHandler.getParsedElements();
		Element el = elems == null ? null : elems.poll();
		return Avatar.parse(el);
	}
	
	public void setItem(@NonNull Avatar avatar) throws TigaseDBException {
		initRepo();
		userRepository.setData(getRepoUserJid(), SUBNODE, avatar.id(), avatar.toElement().toString());
	}

	public void removeItem(@NonNull String key) throws TigaseDBException {
		initRepo();
		userRepository.removeData(getRepoUserJid(), SUBNODE, key);
	}

	public @NonNull CompletableFuture<Void> setRandomAvatarToUser(BareJID userJid) {
		CompletableFuture<Void> result;
		String key = "";
		try {
			List<String> keys = getKeys();
			key = keys.get(random.nextInt(0, keys.size()));
			Avatar avatar = getItem(key);
			if (avatar == null) {
				result = CompletableFuture.failedFuture(
						new RuntimeException("Requested avatar with id = " + key + " was not found!"));
			} else {
				result = userAvatarRepository.setData(userJid, avatar.id(), avatar.data())
						.thenCompose(r -> userAvatarRepository.setMetadata(userJid,
																		   new UserAvatarRepository.AvatarMetadata(
																				   avatar.id(), avatar.mimeType(),
																				   avatar.bytes(), null)));
			}
		} catch (TigaseDBException ex) {
			result = CompletableFuture.failedFuture(ex);
		}
		String id = key;
		return result.whenComplete((r, ex) -> {
			if (ex == null) {
				log.info(() ->  "setting avatar with id " + id + " for user " + userJid + " succeeded!");
			} else {
				log.log(Level.WARNING, ex, () -> "setting avatar with id " + id + " for user " + userJid + " failed!");
			}
		});
	}

	@HandleEvent
	public void onUserAdded(UserRepository.UserAddedEvent event) {
		log.info("received UserAddedEvent for " + event.getJid());
		if (!isEnabled()) {
			return;
		}
		// ensure that user account was added and not a service account
		if (vHostManager.isLocalDomain(event.getJid().getDomain())) {
			log.info("setting random avatar for " + event.getJid());
			setRandomAvatarToUser(event.getJid());
		}
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
		initialized = false;
	}

	private BareJID getRepoUserJid() {
		return module.getJid().getBareJID();
	}

	private void initRepo() {
		if (initialized) {
			return;
		}
		if (!userRepository.userExists(getRepoUserJid())) {
			try {
				userRepository.addUser(getRepoUserJid());
			} catch (UserExistsException ex) {
				// if user already exists, then it could be created by other cluster node.
			} catch (TigaseDBException ex) {
				throw new RuntimeException(ex);
			}
			initialized = true;
		}
	}

	public record Avatar(@NonNull String id, @NonNull String mimeType, long bytes, byte @NonNull[] data) {

		public static @Nullable Avatar parse(@Nullable Element element) {
			if (element == null || !Objects.equals("avatar", element.getName())) {
				return null;
			}

			String id = element.getAttributeStaticStr("id");
			String type = element.getAttributeStaticStr("type");
			long bytes = Optional.ofNullable(element.getAttributeStaticStr("bytes")).map(Long::parseLong).orElse(-1L);
			byte[] data = Optional.ofNullable(element.getCData()).map(Base64::decode).orElse(null);

			if (id == null || type == null || bytes <= 0 || data == null) {
				return null;
			}
			return new Avatar(id, type, bytes, data);
		}

		public @NonNull Element toElement() {
			Element result = new Element("avatar").withAttribute("id", id).withAttribute("type", mimeType).withAttribute("bytes", String.valueOf(bytes));
			result.setCData(Base64.encode(data));
			return result;
		}

	}
}
