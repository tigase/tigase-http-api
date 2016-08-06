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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.script.Bindings;

import tigase.http.modules.Module;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.script.CommandIfc;

public class CommandManager {
	
	private static final Logger log = Logger.getLogger(CommandManager.class.getCanonicalName());
	
	private Map<String,CommandIfc> scriptCommands = new ConcurrentHashMap<String,CommandIfc>();
	
	private Module module;
	
	public CommandManager(Module module) {
		this.module = module;
	}

	public Collection<CommandIfc> getCommands() {
		return scriptCommands.values();
	}
	
	public void registerCmd(CommandIfc cmd) {
		scriptCommands.put(cmd.getCommandId(), cmd);
	}
	
	public void unregisterCmd(CommandIfc cmd) {
		scriptCommands.remove(cmd.getCommandId());
	}
	
	public boolean execute(Packet pc) {
		Iq             iqc    = (Iq) pc;
		Command.Action action = Command.getAction(iqc);

		if (action == Command.Action.cancel) {
			Packet result = iqc.commandResult(Command.DataType.result);

			Command.addTextField(result, "Note", "Command canceled.");
			module.addOutPacket(result);

			return true;
		}

		String     strCommand = iqc.getStrCommand();
		CommandIfc com        = scriptCommands.get(strCommand);
		
		if (com != null) {
			Bindings bindings = com.getBindings();
			if (bindings != null) {
				module.initBindings(bindings);
			}
			Queue<Packet> results = new ArrayDeque<Packet>();
			com.runCommand(iqc, bindings, results);
			for (Packet res : results) {
				module.addOutPacket(res);
			}
			return true;
		}
		return false;
	}
	
}
