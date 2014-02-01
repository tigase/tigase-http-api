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

package tigase.http.rest

import tigase.server.Packet
import tigase.db.UserRepository
import tigase.db.AuthRepository
import tigase.xmpp.BareJID

class ServiceImpl implements Service {

	private final RestModule module;
	
	public ServiceImpl(RestModule module) {
		this.module = module;
	}
	
    void sendPacket(Packet packet, Long timeout, Closure closure) {
		module.addOutPacket(packet, (Integer) (timeout == null ? null : timeout.intValue()),
			new ClosureCallback(closure));
	}
	
    UserRepository getUserRepository() {
		return module.getUserRepository();
	}
	
    AuthRepository getAuthRepository() {
		return module.getAuthRepository();
	}
	
    boolean isAdmin(BareJID user) {
		return true;
	}
	
	boolean isAllowed(String key, String domain, String path) {
		return module.isRequestAllowed(key, domain, path);
	}
	
}

