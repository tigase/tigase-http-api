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
package tigase.http.upload.store;

import tigase.xmpp.BareJID;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by andrzej on 08.08.2016.
 */
public interface Store {

	/**
	 * Returns number of elements in store
	 *
	 * @return
	 */
	long count() throws IOException;

	/**
	 * Returns size used by store
	 *
	 * @return
	 */
	long size() throws IOException;

	/**
	 * Method to retrieve content of file from slot
	 *
	 * @param uploader
	 * @param slotId
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	ReadableByteChannel getContent(BareJID uploader, String slotId, String filename) throws IOException;

	/**
	 * Method to set content of file to slot
	 *
	 * @param uploader
	 * @param slotId
	 * @param filename
	 * @param size
	 * @param source
	 * @throws IOException
	 */
	void setContent(BareJID uploader, String slotId, String filename, long size, ReadableByteChannel source) throws IOException;

	/**
	 * Method removes content of slot
	 *
	 * @param uploader
	 * @param slotId
	 * @throws IOException
	 */
	void remove(BareJID uploader, String slotId) throws IOException;
}
