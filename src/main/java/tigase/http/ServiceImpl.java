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
package tigase.http;

import tigase.auth.credentials.Credentials;
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.http.api.Service;
import tigase.http.modules.AbstractBareModule;
import tigase.http.modules.Module;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ServiceImpl<T extends Module>
		implements Service<T> {

	public ServiceImpl(String moduleUUID) {
		this((T) AbstractBareModule.getModuleByUUID(moduleUUID));
	}

	public ServiceImpl(T module) {
		this.module = module;
	}

	public void sendPacket(Packet packet) {
		module.addOutPacket(packet);
	}

	public CompletableFuture<Packet> sendPacketAndAwait(Packet packet, Long timeout) {
		return module.addOutPacket(packet, (Integer) (timeout == null ? null : timeout.intValue()));
	}

	public UserRepository getUserRepository() {
		return module.getUserRepository();
	}

	public AuthRepository getAuthRepository() {
		return module.getAuthRepository();
	}

	public boolean isAdmin(BareJID user) {
		return module.isAdmin(user);
	}

	public boolean isAllowed(String key, String domain, String path) {
		return module.isRequestAllowed(key, domain, path);
	}

	public boolean checkCredentials(String user, final String password)
			throws TigaseStringprepException, TigaseDBException, AuthorizationException {
		BareJID jid = BareJID.bareJIDInstance(user);
		Credentials credentials = module.getAuthRepository().getCredentials(jid, Credentials.DEFAULT_USERNAME);
		if (credentials == null) {
			return false;
		}

		return Optional.ofNullable(credentials.getFirst()).map(e -> e.verifyPlainPassword(password)).orElse(false);
	}

	public T getModule() {
		return module;
	}

	private final T module;
}
