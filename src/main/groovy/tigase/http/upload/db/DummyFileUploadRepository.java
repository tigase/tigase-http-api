/*
 * Tigase HTTP API
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.upload.db;

import tigase.db.DataSource;
import tigase.db.TigaseDBException;
import tigase.kernel.beans.Bean;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by andrzej on 08.08.2016.
 */
@Bean(name = "repo", exportable = true, active = true)
public class DummyFileUploadRepository implements FileUploadRepository {

	@Override
	public Slot allocateSlot(JID sender, String slotId, String filename, long filesize, String contentType) throws TigaseDBException {
		return new Slot(sender.getBareJID(), slotId, filename, filesize, contentType, new Date());
	}

	@Override
	public void updateSlot(BareJID sender, String slotId) throws TigaseDBException {
	}

	@Override
	public Slot getSlot(BareJID sender, String slotId) throws TigaseDBException {
		return new Slot(sender, slotId, null, -1, null, new Date());
	}

	@Override
	public List<FileUploadRepository.Slot> listExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException {
		return Collections.emptyList();
	}

	@Override
	public void removeExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException {
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		// nothing to do
	}

	public class Slot extends FileUploadRepository.Slot {

		public Slot(BareJID uploader, String slotId, String filename, long filesize, String contentType, Date timestamp) {
			super(uploader, slotId, filename, filesize, contentType, timestamp);
		}

		@Override
		public boolean matches(String slotId, long filesize, String contentType) {
			return this.slotId.equals(slotId);
		}
	}
}
