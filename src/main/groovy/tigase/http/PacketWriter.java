/*
 * Tigase HTTP API
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.http;

import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.http.rest.ApiKeyRepository;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.JID;

public interface PacketWriter {
	
	public static interface Callback {
		
		public void onResult(Packet packet);
		
	}

	UserRepository getUserRepository();
	
	AuthRepository getAuthRepository();
	
	boolean isAdmin(JID user);
	
	public boolean write(Module module, Packet packet);
	
	public boolean write(Module module, Packet packet, Integer timeout, Callback callback);
	
	public ApiKeyRepository getApiKeyRepository();
	
}
