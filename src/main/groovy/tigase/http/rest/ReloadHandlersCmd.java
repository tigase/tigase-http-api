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
package tigase.http.rest;

import java.util.Queue;
import javax.script.Bindings;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.script.CommandIfc;
import tigase.stats.StatisticHolderImpl;

public class ReloadHandlersCmd extends StatisticHolderImpl implements CommandIfc {

	private final RestModule module;
	
	public ReloadHandlersCmd(RestModule restModule) {
		module = restModule;
	}
	
	@Override
	public Bindings getBindings() {
		return null;
	}

	@Override
	public String getCommandId() {
		return "reload-handlers";
	}

	@Override
	public String getDescription() {
		return "Reload REST HTTP script handlers";
	}
	
	@Override
	public String getGroup() {
		return "Configuration";
	}

	@Override
	public void init(String id, String description, String group) {}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public void runCommand(Iq packet, Bindings binds, Queue<Packet> results) {		
		module.start();
		Packet result = packet.commandResult(Command.DataType.result);
		Command.addNote(result, "Script handlers reloaded");
		results.add(result);
	}

	@Override
	public void setAdminOnly(boolean adminOnly) {}
	
}
