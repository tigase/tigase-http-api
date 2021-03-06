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
package tigase.http.upload.db;

import tigase.db.DataSource;
import tigase.db.DataSourceAware;
import tigase.db.TigaseDBException;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Created by andrzej on 07.08.2016.
 */
public interface FileUploadRepository<DS extends DataSource>
		extends DataSourceAware<DS> {

	/**
	 * Create slot in database for file upload.
	 */
	Slot allocateSlot(JID sender, String slotId, String filename, long filesize, String contentType)
			throws TigaseDBException;

	/**
	 * Looks for slot for particular sender with exact slot id, file name and file size
	 */
	void updateSlot(BareJID sender, String slotId) throws TigaseDBException;

	/**
	 * Retrieves information from database about slot
	 */
	Slot getSlot(BareJID sender, String slotId) throws TigaseDBException;

	/**
	 * Retrieves list of ids of expired slots
	 */
	List<Slot> listExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException;

	/**
	 * Removes metadata of expired slots
	 */
	void removeExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException;

	class Slot {

		public final String contentType;
		public final String filename;
		public final long filesize;
		public final String slotId;
		public final Date timestamp;
		public final BareJID uploader;

		public Slot(BareJID uploader, String slotId, String filename, long filesize, String contentType,
					Date timestamp) {
			this.uploader = uploader;
			this.slotId = slotId;
			this.filename = filename;
			this.filesize = filesize;
			this.contentType = contentType;
			this.timestamp = timestamp;
		}

		public boolean matches(String slotId, long filesize, String contentType) {
			return (this.slotId.equals(slotId) && this.filesize == filesize) &&
					(this.contentType == null || this.contentType.equals(contentType));
		}

	}
}
