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
package tigase.http.modules.rest;

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.script.CommandIfc;
import tigase.stats.StatisticHolderImpl;

import javax.script.Bindings;
import java.util.function.Consumer;

public abstract class AbstractApiKeyCmd extends StatisticHolderImpl
		implements CommandIfc {

	protected static final String MARKER = "command-marker";

	protected final RestModule module;

	public AbstractApiKeyCmd(RestModule module) {
		this.module = module;
	}

	@Override
	public Bindings getBindings() {
		return null;
	}
	

	@Override
	public void init(String id, String description, String group) {
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public void setAdminOnly(boolean adminOnly) {
	}

	protected boolean checkIsFromAdmin(Iq packet, Consumer<Packet> writer) {
		if (module.isAdmin(packet.getStanzaFrom().getBareJID())) {
			return true;
		}

		Packet result = packet.commandResult(Command.DataType.result);
		Command.addTextField(result, "Error", "You need to be service administrator to execute this command!");
		writer.accept(result);

		return false;
	}
}
