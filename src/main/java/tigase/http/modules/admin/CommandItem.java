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
package tigase.http.modules.admin;

import tigase.xmpp.jid.JID;

import java.util.Map;
import java.util.Optional;

public class CommandItem {

	private final Map<String,String> attrs;

	public CommandItem(Map<String,String> attrs) {
		this.attrs = attrs;
	}

	public JID getJid() {
		return JID.jidInstanceNS(attrs.get("jid"));
	}

	public String getName() {
		return attrs.get("name");
	}

	public void setName(String name) {
		attrs.put("name", name);
	}

	public String getNode() {
		return attrs.get("node");
	}

	public String getGroup() {
		return Optional.ofNullable(attrs.get("group")).orElse("Other");
	}

}
