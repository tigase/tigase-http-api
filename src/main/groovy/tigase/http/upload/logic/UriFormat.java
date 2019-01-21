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

import tigase.xmpp.jid.JID;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by andrzej on 09.08.2016.
 */
public class UriFormat {

	private final HashSet<String> groups = new HashSet<>();
	private final Pattern pattern;
	private final String template;

	public UriFormat(String format) {
		this.template = format;

		int jidIdx = format.indexOf("{userJid}");
		int domainIdx = format.indexOf("{domain}");
		int slotIdx = format.indexOf("{slotId}");
		int filenameIdx = format.indexOf("{filename}");

		int idx = Math.min(slotIdx, filenameIdx);
		if (idx < 0) {
			throw new RuntimeException("Invalid URI format - must contain {slotId} and {filename}");
		}

		groups.add("slotId");
		groups.add("filename");

		if (jidIdx > -1) {
			idx = Math.min(idx, jidIdx);
			groups.add("jid");
		}

		if (domainIdx > -1) {
			idx = Math.min(idx, domainIdx);
			groups.add("domain");
		}

		String infoTemp = format.substring(idx).replace("/", "\\/");
		infoTemp = infoTemp.replace("{userJid}", "(?<jid>[^/]+)")
				.replace("{slotId}", "(?<slotId>[^/]+)")
				.replace("{filename}", "(?<filename>[^/]+)");

		pattern = Pattern.compile(infoTemp);
	}

	public String formatUri(DefaultLogic.HttpProtocol protocol, String serverName, int port, JID requester,
							String slotId, String filename) {
		String format = template;
		return format.replace("{proto}", protocol.name())
				.replace("{serverName}", serverName)
				.replace("{port}", String.valueOf(port))
				.replace("{domain}", requester.getDomain())
				.replace("{userJid}", requester.getBareJID().toString())
				.replace("{slotId}", slotId)
				.replace("{filename}", filename);
	}

	public Matcher parsePath(String path) {
		return pattern.matcher(path);
	}

	public boolean hasGroup(String group) {
		return groups.contains(group);
	}

	public String getFormat() {
		return template;
	}
}
