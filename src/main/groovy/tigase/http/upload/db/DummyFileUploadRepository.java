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
import tigase.db.TigaseDBException;
import tigase.kernel.beans.Bean;
import tigase.xmpp.jid.BareJID;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by andrzej on 08.08.2016.
 */
@Bean(name = "repo", exportable = true, active = true)
public class DummyFileUploadRepository
		implements FileUploadRepository {

	@Override
	public FileUploadRepository.Slot allocateSlot(BareJID sender, String slotId, String filename, long filesize,
												  String contentType) throws TigaseDBException {
		return new Slot(sender, slotId, filename, filesize, contentType, new Date());
	}

	@Override
	public void updateSlot(BareJID sender, String slotId) throws TigaseDBException {
	}

	@Override
	public Slot getSlot(BareJID sender, String slotId) throws TigaseDBException {
		return new Slot(sender, slotId, null, -1, null, new Date());
	}

	@Override
	public List<FileUploadRepository.Slot> listExpiredSlots(BareJID domain, LocalDateTime before, int limit)
			throws TigaseDBException {
		return Collections.emptyList();
	}

	@Override
	public void removeExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException {
	}

	@Override
	public long getUsedSpaceForDomain(String domain) throws TigaseDBException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getUsedSpaceForUser(BareJID user) throws TigaseDBException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FileUploadRepository.Slot> querySlots(BareJID user, String afterId, int limit)
			throws TigaseDBException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FileUploadRepository.Slot> querySlots(String domain, String afterId, int limit)
			throws TigaseDBException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeSlot(BareJID user, String slotId) throws TigaseDBException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		// nothing to do
	}

	public class Slot
			extends FileUploadRepository.Slot {

		public Slot(BareJID uploader, String slotId, String filename, long filesize, String contentType,
					Date timestamp) {
			super(uploader, slotId, filename, filesize, contentType, timestamp);
		}

		@Override
		public boolean matches(String slotId, long filesize, String contentType) {
			return this.slotId.equals(slotId);
		}
	}
}
