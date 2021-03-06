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
import java.util.Queue;

public class ApiKeyRemoveCmd
		extends AbstractApiKeyCmd {

	private static final String ITEMS = "item-list";

	public ApiKeyRemoveCmd(RestModule module) {
		super(module);
	}

	@Override
	public String getCommandId() {
		return "api-key-remove";
	}

	@Override
	public String getDescription() {
		return "Remove API key";
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

		String itemKey = Command.getFieldValue(packet, ITEMS);
		if (itemKey == null) {
			String[] itemsStr = module.getApiKeyRepository()
					.allItems()
					.stream()
					.map(ApiKeyItem::getKey)
					.toArray(String[]::new);

			if (itemsStr.length > 0) {
				Iq result = (Iq) packet.commandResult(Command.DataType.form);
				Command.addFieldValue(result, ITEMS, itemsStr[0], "List of items", itemsStr, itemsStr);
				results.offer(result);
			} else {
				Iq result = (Iq) packet.commandResult(Command.DataType.result);
				Command.addTextField(result, "Note", "There are no items on the list");
				results.offer(result);
			}
			return;
		}

		Iq result = (Iq) packet.commandResult(Command.DataType.result);
		ApiKeyItem item = module.getApiKeyRepository().getItem(itemKey);
		if (item == null) {
			Command.addTextField(result, "Error", "No such item, deletion impossible.");
		} else {
			module.getApiKeyRepository().removeItem(itemKey);
			Command.addTextField(result, "Note", "Operation successful");
		}

		results.add(result);
	}

}