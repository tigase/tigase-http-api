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
package tigase.http.api;

import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.http.modules.Module;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import java.util.concurrent.CompletableFuture;

public interface Service<T extends Module> {

	void sendPacket(Packet packet);

	default CompletableFuture<Packet> sendPacketAndAwait(Packet packet) {
		return sendPacketAndAwait(packet,null);
	}

	CompletableFuture<Packet> sendPacketAndAwait(Packet packet, Long timeout);
	
	UserRepository getUserRepository();

	AuthRepository getAuthRepository();

	boolean isAdmin(BareJID user);

	boolean isAllowed(String key, String domain, String path);

	boolean checkCredentials(String user, String password)
			throws TigaseStringprepException, TigaseDBException, AuthorizationException;

	T getModule();
}
