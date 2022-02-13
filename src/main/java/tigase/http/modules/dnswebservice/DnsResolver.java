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

import tigase.util.cache.SimpleCache;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author andrzej
 */
public class DnsResolver {

	public static final Logger log = Logger.getLogger(DnsResolver.class.getName());

	private static final long DNS_CACHE_TIME = 1000 * 60;
	//private static ConcurrentMap<String, DnsItem> cache = new ConcurrentHashMap<String, DnsItem>();
	private static Map<String, Future<DnsItem>> cache = Collections.synchronizedMap(
			new SimpleCache<String, Future<DnsItem>>(500, DNS_CACHE_TIME));
	private static ExecutorService executor = Executors.newCachedThreadPool();

	private static List<DnsEntry> addressesToDnsEntries(InetAddress[] addrs, int port, int priority) {
		List<DnsEntry> entries = new ArrayList<DnsEntry>();
		Map<String, List<String>> hostnameToIps = new HashMap<String, List<String>>();
		for (InetAddress addr : addrs) {
			try {
				InetAddress host = InetAddress.getByAddress(addr.getAddress());
				String hostname = host.getHostName();
				List<String> tmp = hostnameToIps.get(hostname);
				if (tmp == null) {
					tmp = new ArrayList<String>();
					hostnameToIps.put(hostname, tmp);
				}
				tmp.add(addr.getHostAddress());
			} catch (UnknownHostException ex) {
			}
		}

		for (String host : hostnameToIps.keySet()) {
			List<String> ipsList = hostnameToIps.get(host);
			String[] ips = ipsList.toArray(new String[ipsList.size()]);
			DnsEntry entry = new DnsEntry(host, port, ips, priority);
			entries.add(entry);
		}
		return entries;
	}

	public static DnsItem get(String domain) {
		try {
			Future<DnsItem> item = cache.get(domain);
			if (item != null) {
				return item.get();
			}

			ResolverTask resolver = new ResolverTask(domain);
			synchronized (cache) {
				item = cache.get(domain);
				if (item == null) {
					item = executor.submit(resolver);
					cache.put(domain, item);
				}
			}

			return item.get();
		} catch (Exception ex) {
			cache.remove(domain);
		}
		return null;
	}

	public static void main(String[] argv) {
		get("hi-low.eu");
	}

	private static DnsEntry parseSrvEntry(String str) throws UnknownHostException {
		String[] parts = str.split(" ");
		int priority = 0;
		int weight = 0;
		int port = 5222;
		String host = parts[3];
		if (host.endsWith(".")) {
			host = host.substring(0, host.length() - 1);
		}

		try {
			priority = Integer.parseInt(parts[0]);
		} catch (Exception ex) {
		}
		try {
			weight = Integer.parseInt(parts[1]);
		} catch (Exception ex) {
		}
		try {
			port = Integer.parseInt(parts[2]);
		} catch (Exception ex) {
		}

		InetAddress[] addrs = InetAddress.getAllByName(host);
		String[] ips = new String[addrs.length];
		for (int i = 0; i < addrs.length; i++) {
			ips[i] = addrs[i].getHostAddress();
		}
		return new DnsEntry(host, port, ips, priority);
		//return addressesToDnsEntries(addrs, port, priority);
	}

	private static DnsItem resolve(String domain) {
		DirContext ctx = null;
		try {
			Hashtable<String, String> env = new Hashtable<String, String>(5);
			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

			ctx = new InitialDirContext(env);
			DnsEntry[] c2sEntries = resolveC2S(ctx, domain);
			if (c2sEntries == null) {
				return null;
			}

			DnsEntry[] boshEntries = resolveBosh(ctx, domain, c2sEntries);
			DnsEntry[] websocketEntries = resolveWebSocket(ctx, domain, c2sEntries);

			return new DnsItem(domain, c2sEntries, boshEntries, websocketEntries);
		} catch (Exception e) {
			//result_host = hostname;
			return null;
		} // end of try-catch
		finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException ex) {
					Logger.getLogger(DnsResolver.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	private static DnsEntry[] resolveBosh(DirContext ctx, String domain, DnsEntry[] c2sEntries) {
		try {
			Attributes attrs = ctx.getAttributes("_xmppconnect." + domain, new String[]{"TXT"});
			Attribute attr = attrs.get("TXT");
			log.log(Level.FINE, "checking bosh TXT for = _xmppconnect." + domain + ", result: " + attr);

			if (attr != null && attr.size() > 0) {
				List<DnsEntry> entries = new ArrayList<DnsEntry>();
				for (int i = 0; i < attr.size(); i++) {
					String data = attr.get(i).toString();

					if (!data.startsWith("_xmpp-client-xbosh")) {
						continue;
					}

					String uri = data.replace("_xmpp-client-xbosh=", "");
					entries.add(new DnsEntry(uri, 0));
				}
				log.log(Level.FINEST, "Adding BOSH DNS entries" + entries + " for domain: " + domain);
				if (!entries.isEmpty()) {
					return entries.toArray(new DnsEntry[entries.size()]);
				}
			}
		} catch (NamingException ex) {
			log.log(Level.FINEST, "no TXT record for domain: " + domain);
		}
		if (c2sEntries != null) {
			List<DnsEntry> entries = new ArrayList<DnsEntry>();
			for (DnsEntry c2sEntry : c2sEntries) {
				DnsEntry[] tmp = resolveBosh(ctx, c2sEntry.getHost(), null);
				if (tmp != null) {
					for (DnsEntry entry : tmp) {
						entries.add(entry);
					}
				}
			}
			log.log(Level.FINEST, "Adding BOSH DNS entries" + entries + " for domain: " + domain);
			if (!entries.isEmpty()) {
				return entries.toArray(new DnsEntry[entries.size()]);
			}
		}
		return null;
	}

	private static DnsEntry[] resolveC2S(DirContext ctx, String domain) {
		try {
			Attributes attrs = ctx.getAttributes("_xmpp-client._tcp." + domain, new String[]{"SRV"});
			Attribute attr = attrs.get("SRV");
			log.log(Level.FINE, "checking c2s SRV for = _xmpp-client._tcp." + domain + ", result: " + attr);

			if (attr != null && attr.size() > 0) {
				List<DnsEntry> entries = new ArrayList<DnsEntry>();
				for (int i = 0; i < attr.size(); i++) {
					String str = attr.get(i).toString();
					try {
						DnsEntry entry = parseSrvEntry(str);
						entries.add(entry);
					} catch (UnknownHostException ex) {
						// we ignore this record if we get UnknownHostException
					}
				}
				return entries.toArray(new DnsEntry[entries.size()]);
			}
		} catch (NamingException ex) {
			log.log(Level.FINEST, "no SRV record for domain - we need to go by A record for domain: " + domain);
		}

		try {
			InetAddress[] addrs = InetAddress.getAllByName(domain);
			//List<DnsEntry> entries = addressesToDnsEntries(addrs, 5222, 0);
			//return entries.toArray(new DnsEntry[entries.size()]);
			String[] ips = new String[addrs.length];
			for (int i = 0; i < addrs.length; i++) {
				ips[i] = addrs[i].getHostAddress();
			}
			return new DnsEntry[]{new DnsEntry(domain, 5222, ips, 0)};
		} catch (UnknownHostException ex) {
			log.log(Level.FINEST, "even no A record for domain - we need to ignore this domain: " + domain);
		}

		return null;
	}

	private static DnsEntry[] resolveWebSocket(DirContext ctx, String domain, DnsEntry[] c2sEntries) {
		try {
			Attributes attrs = ctx.getAttributes("_xmppconnect." + domain, new String[]{"TXT"});
			Attribute attr = attrs.get("TXT");
			log.log(Level.FINE, "checking websocket TXT for = _xmppconnect." + domain + ", result: " + attr);

			if (attr != null && attr.size() > 0) {
				List<DnsEntry> entries = new ArrayList<DnsEntry>();
				for (int i = 0; i < attr.size(); i++) {
					String data = attr.get(i).toString();

					if (!data.startsWith("_xmpp-client-websocket")) {
						continue;
					}

					String uri = data.replace("_xmpp-client-websocket=", "");
					entries.add(new DnsEntry(uri, 0));
				}
				log.log(Level.FINEST, "Adding WebSocket DNS entries" + entries + " for domain: " + domain);
				if (!entries.isEmpty()) {
					return entries.toArray(new DnsEntry[entries.size()]);
				}
			}
		} catch (NamingException ex) {
			log.log(Level.FINEST, "no TXT record for domain: " + domain);
		}
		if (c2sEntries != null) {
			List<DnsEntry> entries = new ArrayList<DnsEntry>();
			for (DnsEntry c2sEntry : c2sEntries) {
				DnsEntry[] tmp = resolveWebSocket(ctx, c2sEntry.getHost(), null);
				if (tmp != null) {
					for (DnsEntry entry : tmp) {
						entries.add(entry);
					}
				}
			}
			log.log(Level.FINEST, "Adding WebSocket DNS entries" + entries + " for domain: " + domain);
			if (!entries.isEmpty()) {
				return entries.toArray(new DnsEntry[entries.size()]);
			}
		}
		return null;
	}

	private static class ResolverTask
			implements Callable<DnsItem> {

		private final String domain;

		public ResolverTask(String domain) {
			this.domain = domain;
		}

		@Override
		public DnsItem call() throws Exception {
			return DnsResolver.resolve(domain);
		}

	}
}
