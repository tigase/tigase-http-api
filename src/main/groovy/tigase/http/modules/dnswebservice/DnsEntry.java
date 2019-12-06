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
package tigase.http.modules.dnswebservice;

public class DnsEntry {

	private String host;
	private String[] ips;
	private int port;
	private int priority;
	private String url;

	public DnsEntry(String host, int port, String[] ips, int priority) {
		this.host = host;
		this.port = port;
		this.ips = ips;
		this.priority = priority;
		this.url = null;
	}

	public DnsEntry(String url, int priority) {
		this.url = url;
		this.priority = priority;
		this.host = null;
		this.port = 0;
		this.ips = null;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String[] getIPs() {
		return ips;
	}

	public int getPriority() {
		return priority;
	}

	public String getURL() {
		return url;
	}

}
