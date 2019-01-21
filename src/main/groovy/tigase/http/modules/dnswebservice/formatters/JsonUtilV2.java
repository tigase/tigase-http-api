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
package tigase.http.modules.dnswebservice.formatters;

import tigase.http.modules.dnswebservice.DnsEntry;
import tigase.http.modules.dnswebservice.DnsItem;

public class JsonUtilV2 {

	public static String format(DnsItem item) {
		StringBuilder sb = new StringBuilder();
		format(sb, item);
		return sb.toString();
	}

	public static void format(StringBuilder sb, DnsItem item) {
		if (item == null) {
			sb.append("null");
			return;
		}

		sb.append("{ domain: '");
		sb.append(item.getDomain());
		sb.append("', c2s: [");
		format(sb, item.getC2S());
		sb.append("], bosh: [");
		format(sb, item.getBosh());
		sb.append("], websocket: [");
		format(sb, item.getWebSocket());
		sb.append("]");

		sb.append("}");
	}

	public static void format(StringBuilder sb, DnsEntry[] entries) {
		if (entries != null && entries.length > 0) {
			for (int i = 0; i < entries.length; i++) {
				if (i > 0) {
					sb.append(",");
				}

				format(sb, entries[i]);
			}
		}
	}

	public static void format(StringBuilder sb, DnsEntry entry) {
		sb.append("{");
		int i = 0;
		if (entry.getHost() != null) {
			sb.append("host: '");
			sb.append(entry.getHost());
			sb.append("'");
			i++;
		}
		if (entry.getIPs() != null) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("ip: [");
			int j = 0;
			for (String ip : entry.getIPs()) {
				if (j > 0) {
					sb.append(",");
				}
				sb.append("'");
				sb.append(ip);
				sb.append("'");
				j++;
			}
			sb.append("]");
			i++;
		}
		if (entry.getPort() != 0) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("port: ");
			sb.append(entry.getPort());
			i++;
		}
		if (entry.getURL() != null) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("url:'");
			sb.append(entry.getURL());
			sb.append("'");
			i++;
		}
		if (entry.getPriority() != 0) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("priority: ");
			sb.append(entry.getPriority());
		}
		sb.append("}");
	}
}
