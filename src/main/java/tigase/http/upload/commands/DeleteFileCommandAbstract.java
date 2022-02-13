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
import tigase.component.exceptions.RepositoryException;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.http.upload.db.FileUploadRepository;
import tigase.http.upload.store.Store;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.IOException;
import java.util.Optional;

public abstract class DeleteFileCommandAbstract
		implements AdHocCommand {

	@Inject
	private FileUploadRepository repository;
	@Inject
	private Store store;
	@Inject
	private AdHocCommandModule.ScriptCommandProcessor scriptCommandProcessor;

	private final boolean isAdmin;

	protected DeleteFileCommandAbstract(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		if (request.isAction("cancel")) {
			response.cancelSession();
			return;
		}

		Optional<String> slotId = Optional.ofNullable(Command.getFieldValue(request.getIq(), "slot-id"));
		Optional<BareJID> user = isAdmin ? Optional.ofNullable(Command.getFieldValue(request.getIq(), "jid")).map(BareJID::bareJIDInstanceNS) : Optional.of(request.getSender().getBareJID());

		if (slotId.isEmpty() || user.isEmpty()) {
			DataForm.Builder builder = new DataForm.Builder(Command.DataType.form).addInstructions(
					new String[]{"Please fill the form"});
			if (isAdmin) {
				builder.withField(DataForm.FieldType.JidSingle, "jid", field -> field.setLabel("JID").setValue(user.map(BareJID::toString).orElse(null)));
			}
			builder.withField(DataForm.FieldType.TextSingle, "slot-id",
							   field -> field.setLabel("Slot ID").setValue(slotId.orElse(null)));
			response.getElements().add(builder.build());
			response.setNewState(AdHocResponse.State.executing);
		} else {
			try {
				Optional<FileUploadRepository.Slot> slot = Optional.ofNullable(repository.getSlot(user.get(), slotId.get()));
				if (slot.isEmpty()) {
					throw new AdHocCommandException(Authorization.FORBIDDEN);
				}

				if ((!request.getSender().getBareJID().equals(user.get())) && (!scriptCommandProcessor.isAllowed(getNode(), user.get().getDomain(), request.getSender()))) {
					throw new AdHocCommandException(Authorization.FORBIDDEN);
				}

				store.remove(user.get(), slotId.get());
				repository.removeSlot(user.get(), slotId.get());
			} catch (UnsupportedOperationException ex) {
				throw new AdHocCommandException(Authorization.FEATURE_NOT_IMPLEMENTED);
			} catch (RepositoryException | IOException ex) {
				throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR);
			}
		}
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return this.scriptCommandProcessor.isAllowed(this.getNode(), jid);
	}
}
