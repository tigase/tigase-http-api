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

public class JsonUtilV1 {

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
		sb.append("', entries: [");
		if (item.getC2S() != null) {
			int i = 0;
			for (DnsEntry entry : item.getC2S()) {
				for (String ip : entry.getIPs()) {
					if (i > 0) {
						sb.append(",");
					}
					sb.append("{port:5280, ip:'");
					sb.append(ip);
					sb.append("', resultHost: '");
					sb.append(entry.getHost());
					sb.append("'}");
					i++;
				}
			}
		}
		sb.append("]}");
	}

}
