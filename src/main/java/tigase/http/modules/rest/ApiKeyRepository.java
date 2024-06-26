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
package tigase.http.modules.rest;

import tigase.db.DBInitException;
import tigase.db.UserRepository;
import tigase.db.comp.UserRepoRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.http.modules.AbstractModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.xmpp.jid.BareJID;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "repository", parent = AbstractModule.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class ApiKeyRepository
		extends UserRepoRepository<ApiKeyItem> {

	public static final String API_KEYS_KEY = "api-keys";
	private static final String GEN_API_KEYS = "--api-keys";
	private final static Logger log = Logger.getLogger(ApiKeyRepository.class.getName());
	
	private ApiKeyItem bootstrapApiKeyItem = null;

	@ConfigField(desc = "Configure REST API to be open and avoid requirement for api-key", alias = "open-access")
	private boolean openAccess = false;

	private BareJID repoUserJid;

	@Inject
	private EventBus eventBus;

	@Override
	public BareJID getRepoUser() {
		return repoUserJid;
	}

	public void setRepoUser(BareJID repoUserJid) {
		this.repoUserJid = repoUserJid;
	}

	@Override
	public String getConfigKey() {
		return API_KEYS_KEY;
	}

	@Override
	public String[] getDefaultPropetyItems() {
		return new String[0];
	}

	@Override
	public String getItemsListPKey() {
		return API_KEYS_KEY;
	}

	@Override
	public String getPropertyKey() {
		return GEN_API_KEYS;
	}

	@Override
	public ApiKeyItem getItemInstance() {
		return new ApiKeyItem();
	}

	public boolean isAllowed(String key, String domain, String path) {
		// allow access for anyone if there is no api key defined
		if (openAccess) {
			return true;
		}

		// if supplied key is null we need to check if for this domain 
		// or path open_access is not set
		if (key == null) {
			key = "open_access";
		}

		ApiKeyItem item = getItem(key);


		if (item != null) {
			// if item exists it will check if access for path is allowed for supplied
			return item.isAllowed(key, domain, path);
		} else {
			// as a fallback, if there is no item configured key,
			// we check if there is maybe an environment variable set
			if (allItems().isEmpty() && bootstrapApiKeyItem != null) {
				log.log(Level.INFO, "Using environment HTTP API-KEY as no other api-key is configured");
				return bootstrapApiKeyItem.isAllowed(key, domain, path);
			} else {
				// if there is no configured item nor env.variable then we deny the access
				return false;
			}
		}
	}
	
	@Override
	public void setRepo(UserRepository userRepository) {
		if (getRepoUser() != null) {
			super.setRepo(userRepository);
		}
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

	@Override
	@Deprecated
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
	}

	@Override
	public void initialize() {
		super.initialize();
		eventBus.registerAll(this);

		var environmentApiKey = System.getenv("HTTP_API_KEY_BOOTSTRAP");
		if (environmentApiKey != null) {
			bootstrapApiKeyItem = new ApiKeyItem();
			bootstrapApiKeyItem.setKey(environmentApiKey);
			log.log(Level.INFO, "Detected environment HTTP API-KEY");
		}
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
		super.beforeUnregister();
	}

	@Override
	public void addItem(ApiKeyItem item) {
		super.addItem(item);
		eventBus.fire(new ItemsChangedEvent(repoUserJid.getLocalpart()));
	}

	@Override
	public void removeItem(String key) {
		super.removeItem(key);
		eventBus.fire(new ItemsChangedEvent(repoUserJid.getLocalpart()));
	}

	@HandleEvent
	public void itemsChanged(ItemsChangedEvent itemsChanged) {
		if (!itemsChanged.checkModule(repoUserJid.getLocalpart())) {
			return;
		}

		super.reload();
	}

	public static class ItemsChangedEvent implements Serializable {

		private String module;
		private transient boolean local = false;
		
		/**
		 * Empty constructor to be able to serialize/deserialize event
		 */
		public ItemsChangedEvent() {
		}

		public ItemsChangedEvent(String module) {
			this.module = module;
		}

		public boolean checkModule(String module) {
			return Objects.equals(this.module, module);
		}

		public boolean isLocal() {
			return local;
		}
	}
}
