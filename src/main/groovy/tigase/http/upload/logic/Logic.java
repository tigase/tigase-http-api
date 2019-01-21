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
package tigase.http.upload.logic;

import tigase.component.exceptions.ComponentException;
import tigase.xmpp.jid.JID;

import java.time.Duration;

/**
 * Created by andrzej on 07.08.2016.
 */
public interface Logic {

	long getMaxFileSize();

	String requestSlot(JID requester, String filename, long filesize, String contentType) throws ComponentException;

	String getUploadURI(JID requester, String slotId, String filename);

	String getDownloadURI(JID requester, String slotId, String filename);

	String generateSlotId();

	UriFormat getUploadURIFormat();

	UriFormat getDownloadURIFormat();

	void removeExpired(Duration expirationTime, int limit);
}
