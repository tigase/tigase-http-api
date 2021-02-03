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
package tigase.http.upload.logic;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import tigase.xmpp.jid.JID;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UriFormatTest {

	@Test
	public void formatUri() {
		UriFormat formatter = new UriFormat("{proto}://{serverName}:{port}/upload/{userJid}/{slotId}/{filename}");
		String specialChar = URLDecoder.decode("%E2%80%93", StandardCharsets.UTF_8);
		String endcodedUrl = formatter.formatUri(DefaultLogic.HttpProtocol.https,"example.com", 443, JID.jidInstanceNS("user@example.com"), "1ebac25b-9a24-4033-8ff2-d189bc832278", "New_Issue_" +
				specialChar + "_YouTrack.png");
		assertFalse(endcodedUrl.contains(specialChar));
	}

}