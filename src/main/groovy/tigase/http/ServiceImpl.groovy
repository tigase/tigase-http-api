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
package tigase.http

import groovy.transform.CompileStatic
import tigase.db.AuthRepository
import tigase.db.AuthorizationException
import tigase.db.TigaseDBException
import tigase.db.UserRepository
import tigase.http.PacketWriter.Callback
import tigase.http.api.Service
import tigase.server.Packet
import tigase.util.TigaseStringprepException
import tigase.xmpp.BareJID

@CompileStatic
public class ServiceImpl<T extends AbstractModule> implements Service<T>, tigase.http.rest.Service {

	private final T module;
	
	public ServiceImpl(String moduleUUID) {
		this((T) AbstractModule.getModuleByUUID(moduleUUID));
	}
	
	public ServiceImpl(T module) {
		this.module = module;
	}
	
	void sendPacket(Packet packet, Long timeout, Callback closure) {
		if (closure != null) {
			module.addOutPacket(packet, (Integer) (timeout == null ? null : timeout.intValue()),
				closure);
		} else {
			module.addOutPacket(packet);
		} 
	}
	
    void sendPacket(Packet packet, Long timeout, Closure closure) {
		if (closure != null) {
			module.addOutPacket(packet, (Integer) (timeout == null ? null : timeout.intValue()),
				new ClosureCallback(closure));
		} else {
			module.addOutPacket(packet);
		}
	}
	
    UserRepository getUserRepository() {
		return module.getUserRepository();
	}
	
    AuthRepository getAuthRepository() {
		return module.getAuthRepository();
	}
	
    boolean isAdmin(BareJID user) {
		return module.isAdmin(user);
	}
	
	boolean isAllowed(String key, String domain, String path) {
		return module.isRequestAllowed(key, domain, path);
	}

	boolean checkCredentials(String user, String password) throws TigaseStringprepException, TigaseDBException, AuthorizationException {
		BareJID jid = BareJID.bareJIDInstance(user);
		return module.getAuthRepository().plainAuth(jid, password);
	}
	
	void executedIn(String path, long executionTime) {
		module.executedIn(path, executionTime);
	}
	
	T getModule() {
		return module;
	}
}

