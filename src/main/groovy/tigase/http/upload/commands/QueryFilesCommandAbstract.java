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
import tigase.http.upload.db.FileUploadRepository;
import tigase.http.upload.logic.Logic;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.util.datetime.TimestampHelper;
import tigase.xml.XMLUtils;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;

public abstract class QueryFilesCommandAbstract implements AdHocCommand {

	private static final TimestampHelper timestampHelper = new TimestampHelper();

	@Inject
	private Logic logic;
	@Inject
	private FileUploadRepository repository;
	@Inject
	private AdHocCommandModule.ScriptCommandProcessor scriptCommandProcessor;

	protected final boolean isAdmin;

	protected QueryFilesCommandAbstract(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		if (request.isAction("cancel")) {
			response.cancelSession();;
			return;
		}
		
		Optional<QueryFilesCommandAdmin.QueryType> queryType = isAdmin ? Optional.ofNullable(
				Command.getFieldValue(request.getIq(), "type")).map(QueryFilesCommandAdmin.QueryType::valueOf) : Optional.of(
				QueryFilesCommandAdmin.QueryType.user);
		Optional<BareJID> user = isAdmin ? Optional.ofNullable(Command.getFieldValue(request.getIq(), "jid")).map(BareJID::bareJIDInstanceNS) : Optional.of(request.getSender().getBareJID());
		Optional<String> domain = isAdmin ? Optional.ofNullable(Command.getFieldValue(request.getIq(), "domain")) : Optional.empty();

		int limit = Optional.ofNullable(Command.getFieldValue(request.getIq(), "limit"))
				.map(Integer::parseInt)
				.orElse(20);
		String afterId = Command.getFieldValue(request.getIq(), "after-id");

		if (queryType.isEmpty() || (queryType.filter(type -> type == QueryType.user).isPresent() && user.isEmpty()) ||
				(queryType.filter(type -> type == QueryType.domain).isPresent() && domain.isEmpty())) {
			DataForm.Builder formBuilder = prepareQueryFormBuilder(queryType, user, domain, afterId, limit);
			response.getElements().add(formBuilder.build());
			response.setNewState(AdHocResponse.State.executing);
		} else {
			try {
				List<FileUploadRepository.Slot> slots = Collections.emptyList();
				switch (queryType.get()) {
					case user:
						if ((!request.getSender().getBareJID().equals(user.get())) && (!scriptCommandProcessor.isAllowed(this.getNode(), user.get().getDomain(), request.getSender()))) {
							throw new AdHocCommandException(Authorization.FORBIDDEN);
						}

						slots = repository.querySlots(user.get(), afterId, limit);
						break;
					case domain:
						if (!scriptCommandProcessor.isAllowed(this.getNode(), domain.get(), request.getSender())) {
							throw new AdHocCommandException(Authorization.FORBIDDEN);
						}

						slots = repository.querySlots(domain.get(), afterId, limit);
						break;
				}

				DataForm.Builder formBuilder = prepareQueryFormBuilder(queryType, user, domain, afterId, limit);

				if (!slots.isEmpty()) {
					formBuilder = formBuilder.withReported(reported -> {
						reported.addField(DataForm.FieldType.TextSingle, "id").setLabel("ID").build();
						reported.addField(DataForm.FieldType.TextSingle, "filename").setLabel("Filename").build();
						reported.addField(DataForm.FieldType.TextSingle, "filesize").setLabel("File size").build();
						reported.addField(DataForm.FieldType.TextSingle, "mimetype").setLabel("MIME Type").build();
						reported.addField(DataForm.FieldType.TextSingle, "timestamp").setLabel("Timestamp").build();
						reported.addField(DataForm.FieldType.TextSingle, "url").setLabel("URL").build();
						if (isAdmin) {
							reported.addField(DataForm.FieldType.JidSingle, "uploader").setLabel("Uploader").build();
						}
					});

					for (FileUploadRepository.Slot slot : slots) {
						formBuilder = formBuilder.withItem(item -> {
							item.addField("id").setValue(slot.slotId).build();
							item.addField("filename").setValue(XMLUtils.escape(slot.filename)).build();
							item.addField("filesize").setValue(String.valueOf(slot.filesize)).build();
							item.addField("mimetype").setValue(XMLUtils.escape(slot.contentType)).build();
							item.addField("timestamp").setValue(timestampHelper.formatWithMs(slot.timestamp)).build();
							item.addField("url")
									.setValue(XMLUtils.escape(logic.getDownloadURI(request.getSender(), slot.slotId, slot.filename)))
									.build();
							if (isAdmin) {
								item.addField("uploader").setValue(slot.uploader.toString()).build();
							}
						});
					}
				}

				response.getElements().add(formBuilder.build());
				response.setNewState(AdHocResponse.State.executing);
			} catch (UnsupportedOperationException ex) {
				throw new AdHocCommandException(Authorization.FEATURE_NOT_IMPLEMENTED);
			} catch (TigaseDBException ex) {
				throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR);
			}
		}
	}

	protected DataForm.Builder prepareQueryFormBuilder(Optional<QueryFilesCommandAdmin.QueryType> queryType, Optional<BareJID> user, Optional<String> domain, String afterId, int limit) {
		DataForm.Builder formBuilder = new DataForm.Builder(Command.DataType.form).addInstructions(
				new String[]{"Please fill the form"});

		if (isAdmin) {
			if (queryType.isEmpty()) {
				formBuilder.withField(DataForm.FieldType.ListSingle, "type", field -> field.setLabel("Type")
						.setOptions(QueryFilesCommandAdmin.QueryType.options(), QueryFilesCommandAdmin.QueryType.names()));
				return formBuilder;
			} else {
				formBuilder.withField(DataForm.FieldType.Hidden, "type",
									  field -> field.setValue(queryType.map(
											  QueryFilesCommandAdmin.QueryType::name).orElse(null)));

				switch (queryType.get()) {
					case user:
						formBuilder.withField(user.isPresent() ? DataForm.FieldType.Fixed : DataForm.FieldType.JidSingle, "jid",
											  field -> field.setValue(user.map(BareJID::toString).orElse(null)));
						break;
					case domain:
						formBuilder.withField(
								domain.isPresent() ? DataForm.FieldType.Fixed : DataForm.FieldType.TextSingle, "domain",
								field -> field.setLabel("Domain").setValue(domain.orElse(null)));
				}
			}
		}

		return addPagingFields(formBuilder, afterId, limit);
	}

	protected DataForm.Builder addPagingFields(DataForm.Builder formBuilder, String afterId, int limit) {
		return formBuilder.withField(DataForm.FieldType.TextSingle, "after-id", field -> field.setLabel("After slot with id").setValue(afterId))
				.withField(DataForm.FieldType.TextSingle, "limit", field -> field.setLabel("Limit of slots").setValue(String.valueOf(limit)));
	}
	
	@Override
	public boolean isAllowedFor(JID jid) {
		return this.scriptCommandProcessor.isAllowed(this.getNode(), jid);
	}

	enum QueryType {
		user,
		domain;

		String getLabel() {
			return name().substring(0,1).toUpperCase() + name().substring(1);
		}

		static String[] options() {
			return Arrays.stream(QueryFilesCommandAdmin.QueryType.values())
					.sorted(Comparator.comparing(QueryFilesCommandAdmin.QueryType::name))
					.map(QueryFilesCommandAdmin.QueryType::name)
					.toArray(String[]::new);
		}

		static String[] names() {
			return Arrays.stream(QueryFilesCommandAdmin.QueryType.values())
					.sorted(Comparator.comparing(QueryFilesCommandAdmin.QueryType::name))
					.map(QueryFilesCommandAdmin.QueryType::getLabel)
					.toArray(String[]::new);
		}


	}

}
