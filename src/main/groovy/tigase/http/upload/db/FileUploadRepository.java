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

import tigase.annotations.TigaseDeprecated;
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
	Slot allocateSlot(BareJID sender, String slotId, String filename, long filesize, String contentType)
			throws TigaseDBException;

	/**
	 * Create slot in database for file upload.
	 */
	@TigaseDeprecated(since = "2.2.0", removeIn = "3.0.0", note = "Use method allocateSlot() with sender as a BareJID")
	@Deprecated
	default Slot allocateSlot(JID sender, String slotId, String filename, long filesize, String contentType)
			throws TigaseDBException {
		return allocateSlot(sender.getBareJID(), slotId, filename, filesize, contentType);
	}
	
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

	/**
	 * Calculates space used by files upload by all users from domain
	 * @param domain
	 * @return
	 * @throws TigaseDBException
	 */
	long getUsedSpaceForDomain(String domain) throws TigaseDBException;

	/**
	 * Calculates space used by files upload by the user
	 * @param jid
	 * @return
	 * @throws TigaseDBException
	 */
	long getUsedSpaceForUser(BareJID user) throws TigaseDBException;

	/**
	 * Retrieves list of slots after slot with provided id. If id is null, first slots will be returned.
	 * @param user
	 * @param afterId
	 * @return
	 * @throws TigaseDBException
	 */
	List<Slot> querySlots(BareJID user, String afterId, int limit) throws TigaseDBException;

	/**
	 * Retrieves list of slots after slot with provided id. If id is null, first slots will be returned.
	 * @param user
	 * @param afterId
	 * @return
	 * @throws TigaseDBException
	 */
	List<Slot> querySlots(String domain, String afterId, int limit) throws TigaseDBException;

	/**
	 * Removes slot with id
	 * @param slotId
	 * @throws TigaseDBException
	 */
	void removeSlot(BareJID user, String slotId) throws TigaseDBException;

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
