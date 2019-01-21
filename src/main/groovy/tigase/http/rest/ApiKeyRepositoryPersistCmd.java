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
package tigase.http.rest;

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.script.CommandIfc;
import tigase.stats.StatisticsList;

import javax.script.Bindings;
import java.util.Queue;

public class ApiKeyRepositoryPersistCmd implements CommandIfc {

	private final ApiKeyRepository apiKeyRepository;

	public ApiKeyRepositoryPersistCmd(ApiKeyRepository apiKeyRepository) {
		this.apiKeyRepository = apiKeyRepository;
	}

	@Override
	public Bindings getBindings() {
		return null;
	}

	@Override
	public String getCommandId() {
		return "api-key-repo-persist";
	}

	@Override
	public String getDescription() {
		return "Persist API keys items configuration";
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
		Iq result = (Iq) packet.commandResult(Command.DataType.result);

		for (ApiKeyItem item : apiKeyRepository.allItems()) {
			apiKeyRepository.addItem(item);
		}
		Command.addTextField(result, "Note", "Operation successful");

		results.add(result);
	}

	@Override
	public void setAdminOnly(boolean adminOnly) {}

	@Override
	public void statisticExecutedIn(long l) {
		
	}

	@Override
	public void everyHour() {

	}

	@Override
	public void everyMinute() {

	}

	@Override
	public void everySecond() {

	}

	@Override
	public void getStatistics(String s, StatisticsList statisticsList) {

	}

	@Override
	public void setStatisticsPrefix(String s) {

	}
}
