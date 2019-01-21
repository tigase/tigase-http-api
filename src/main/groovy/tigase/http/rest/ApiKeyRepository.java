/**
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
package tigase.http.rest;

import java.util.Map;

import tigase.db.DBInitException;
import tigase.db.comp.RepositoryItem;
import tigase.db.comp.UserRepoRepository;

import tigase.xmpp.BareJID;

import java.util.concurrent.ConcurrentSkipListMap;

public class ApiKeyRepository extends UserRepoRepository<ApiKeyItem> {

	private static final String GEN_API_KEYS = "--api-keys";
	public static final String API_KEYS_KEY = "api-keys";
	
	private boolean openAccess = false;
	
	private BareJID repoUserJid;

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
	
	@Override
	protected void initItemsMap() {
		items = new ConcurrentSkipListMap<String, ApiKeyItem>( );
	}

	public boolean isAllowed(String key, String domain, String path) {
		// allow access for anyone if there is no api key defined
		if (openAccess)
			return true;
		
		// if supplied key is null we need to check if for this domain 
		// or path open_access is not set
		if (key == null)
			key = "open_access";
				
		ApiKeyItem item = getItem(key);
		
		// if there is no such key as supplied key we deny access
		if (item == null)
			return false;
		
		// if item exists it will check if access for path is 
		// allowed for supplied key
		return item.isAllowed(key, domain, path);
	}
	
	@Override
	public void setProperties(Map<String,Object> props) {
		String[] items_arr = (String[]) props.get(getConfigKey());	
		if (items_arr != null && items_arr.length == 1 && "open_access".equals(items_arr[0])) {
			openAccess = true;
		}
		else {
			openAccess = false;
		}
		super.setProperties(props);
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
	}
}
