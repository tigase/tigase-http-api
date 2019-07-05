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
package tigase.http.stats;

import tigase.http.HttpMessageReceiver;
import tigase.kernel.beans.Bean;
import tigase.stats.StatisticsList;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

@Bean(name = "host-name-stats-collector", parent = HttpMessageReceiver.class, active = false)
public class HostNameStatsCollector
		implements HttpStatsCollector {

	private final static String key = "host-count/";
	private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

	public HostNameStatsCollector() {
	}

	@Override
	public void getStatistics(String name, StatisticsList list) {
		counters.forEach((host, count) -> {
			list.add(name, key + host, count.get(), Level.FINEST);
		});
	}

	@Override
	public void count(HttpServletRequest request) {
		String host = request.getHeader("Host");
		host = host.contains(":") ? host.substring(0, host.indexOf(':')) : host;
		counters.computeIfAbsent(host, k -> new AtomicLong()).getAndIncrement();
	}

	@Override
	public String toString() {
		return counters.toString();
	}
}