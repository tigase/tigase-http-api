/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.http.rest;

import tigase.db.comp.RepositoryItem;
import tigase.db.comp.UserRepoRepository;
import tigase.xmpp.BareJID;

/**
 *
 * @author andrzej
 */
public class ApiKeyRepository extends UserRepoRepository<ApiKeyItem> {

	private static final String GEN_API_KEYS = "--api-keys";
	private static final String API_KEYS_KEY = "api-keys";
	
	private BareJID repoUserJid;
		
	@Override
	public BareJID getRepoUser() {
		return repoUserJid;
	}
	
	protected void setRepoUser(BareJID repoUserJid) {
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
	public String getPropertyKey() {
		return GEN_API_KEYS;
	}

	@Override
	public ApiKeyItem getItemInstance() {
		return new ApiKeyItem();
	}
	
	public boolean isAllowed(String key, String path) {
		// allow access for anyone if there is no api key defined
		if (this.size() == 0)
			return true;
		
		// if supplied key is null we deny access
		if (key == null)
			return false;
				
		ApiKeyItem item = getItem(key);
		
		// if there is no such key as supplied key we deny access
		if (item == null)
			return false;
		
		// if item exists it will check if access for path is 
		// allowed for supplied key
		return item.isAllowed(key, path);
	}
}
