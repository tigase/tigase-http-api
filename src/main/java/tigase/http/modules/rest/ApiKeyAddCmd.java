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
package tigase.http.modules.rest;

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;

import javax.script.Bindings;
import java.util.Optional;
import java.util.Queue;

public class ApiKeyAddCmd
		extends AbstractApiKeyCmd {

	public ApiKeyAddCmd(RestModule module) {
		super(module);
	}

	@Override
	public String getCommandId() {
		return "api-key-add";
	}

	@Override
	public String getDescription() {
		return "Add API key";
	}

	@Override
	public String getGroup() {
		return "Configuration";
	}

	@Override
	public void runCommand(Iq packet, Bindings binds, Queue<Packet> results) {
		if (!checkIsFromAdmin(packet, results::offer)) {
			return;
		}

		ApiKeyItem item = module.getApiKeyRepository().getItemInstance();
		item.initFromCommand(packet);

		if (Command.getFieldValue(packet, MARKER) == null) {
			Packet result = packet.commandResult(Command.DataType.form);
			Command.addHiddenField(result, MARKER, MARKER);
			item.addCommandFields(result);
			results.offer(result);
			return;
		}

		Packet result = packet.commandResult(Command.DataType.result);


		Optional<ApiKeyItem> oldItem = Optional.ofNullable(item.getKey()).map(module.getApiKeyRepository()::getItem);
		if (oldItem.isPresent()) {
			Command.addTextField(result, "Error", "The item is already added, you can't add it twice.");
		} else {
			String validateResult = module.getApiKeyRepository().validateItem(item);
			if (validateResult == null && Optional.ofNullable(item.getKey()).filter(val -> !val.isEmpty()).isPresent()) {
				module.getApiKeyRepository().addItem(item);
				Command.addTextField(result, "Note", "Operation successful.");
			} else {
				Command.addTextField(result, "Error", "The item did not pass validation checking.");
				Command.addTextField(result, "Note", "   ");
				Command.addTextField(result, "Warning", validateResult);
			}
		}
		results.offer(result);
	}

}