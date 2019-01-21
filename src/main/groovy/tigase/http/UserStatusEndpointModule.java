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
package tigase.http;

import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.xmpp.StanzaType;

public class UserStatusEndpointModule extends AbstractModule {

	@Override
	public String getName() {
		return "user-status-endpoint";
	}

	@Override
	public String getDescription() {
		return "User Status Endpoint";
	}

	@Override
	public boolean isRequestAllowed(String key, String domain, String path) {
		return false;
	}

	@Override
	public UserRepository getUserRepository() {
		return null;
	}

	@Override
	public AuthRepository getAuthRepository() {
		return null;
	}

	@Override
	public boolean processPacket(Packet packet) {
		if (packet.getStanzaTo().getResource() == null) {
			return super.processPacket(packet);
		}

		// if it is presence, then just consume it
		if (packet instanceof Presence) {
			return true;
		}

		StanzaType type = packet.getType();

		// if error or response just consume it
		if (type == StanzaType.error || type == StanzaType.result) {
			return true;
		}

		// we do not know what to do, so lets just return feature-not-implemented
		if (packet instanceof Iq && packet.isCommand() && packet.getCommand() == Command.CHECK_USER_CONNECTION) {
			this.addOutPacket(packet.okResult((String) null, 0));
			return true;
		}
		return false;
	}
}
