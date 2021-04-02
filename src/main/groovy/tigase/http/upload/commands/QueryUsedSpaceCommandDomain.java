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
package tigase.http.upload.commands;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.db.TigaseDBException;
import tigase.http.upload.FileUploadComponent;
import tigase.http.upload.db.FileUploadRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

import java.util.Optional;

@Bean(name = "query-space-used-command-domain", parent = FileUploadComponent.class, active = true)
public class QueryUsedSpaceCommandDomain
		implements AdHocCommand {

	@Inject
	private FileUploadRepository repository;
	@Inject
	private AdHocCommandModule.ScriptCommandProcessor scriptCommandProcessor;

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		if (request.isAction("cancel")) {
			response.cancelSession();
			return;
		}
		try {
			Optional<String> domain = Optional.ofNullable(Command.getFieldValue(request.getIq(), "domain"));
			if (domain.isEmpty()) {
				DataForm.Builder formBuilder = new DataForm.Builder(Command.DataType.form).withField(
						DataForm.FieldType.TextSingle, "domain",
						field -> field.setLabel("Domain"));
				response.getElements().add(formBuilder.build());
				response.setNewState(AdHocResponse.State.executing);
			} else {
				if (!scriptCommandProcessor.isAllowed(this.getNode(), domain.get(), request.getSender())) {
					throw new AdHocCommandException(Authorization.FORBIDDEN);
				}
				long usedSpace = repository.getUsedSpaceForDomain(domain.get());

				DataForm.Builder formBuilder = new DataForm.Builder(Command.DataType.result).withField(
						DataForm.FieldType.TextSingle, "used-space",
						field -> field.setLabel("Used space").setValue(String.valueOf(usedSpace)));
				response.getElements().add(formBuilder.build());
				response.completeSession();
			}
		} catch (TigaseDBException ex) {
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public String getName() {
		return "Query space used by uploaded files in domain";
	}

	@Override
	public String getNode() {
		return "query-space-used-domain";
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return this.scriptCommandProcessor.isAllowed(this.getNode(), jid);
	}
}
