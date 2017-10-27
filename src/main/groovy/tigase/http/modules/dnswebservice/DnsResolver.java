/*
 * Tigase HTTP API
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.modules.dnswebservice;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import tigase.util.cache.SimpleCache;

/**
 *
 * @author andrzej
 */
public class DnsResolver {

	private static final long DNS_CACHE_TIME = 1000 * 60;
	//private static ConcurrentMap<String, DnsItem> cache = new ConcurrentHashMap<String, DnsItem>();
	private static Map<String, Future<DnsItem>> cache = Collections
			.synchronizedMap(new SimpleCache<String, Future<DnsItem>>(500, DNS_CACHE_TIME));
	private static ExecutorService executor = Executors.newCachedThreadPool();

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
		}
		catch (Exception ex) {
			cache.remove(domain);
		}
		return null;
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

	private static DnsEntry[] resolveC2S(DirContext ctx, String domain) {
		try {
			Attributes attrs = ctx.getAttributes("_xmpp-client._tcp." + domain, new String[]{"SRV"});
			Attribute attr = attrs.get("SRV");

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
			// no SRV record for domain - we need to go by A record for domain
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
			// even no A record for domain - we need to ignore this domain
		}

		return null;
	}

	private static DnsEntry[] resolveBosh(DirContext ctx, String domain, DnsEntry[] c2sEntries) {
		try {
			System.out.println("checing bosh for = _xmppconnect." + domain);
			Attributes attrs = ctx.getAttributes("_xmppconnect." + domain, new String[]{"TXT"});
			Attribute attr = attrs.get("TXT");

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
				if (!entries.isEmpty()) {
					return entries.toArray(new DnsEntry[entries.size()]);
				}
			}
		} catch (NamingException ex) {
			// no TXT record for domain
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
			if (entries.isEmpty()) {
				Set<String> hostnames = new HashSet<String>();
				for (DnsEntry c2sEntry : c2sEntries) {
					try {
						InetAddress[] addrs = InetAddress.getAllByName(c2sEntry.getHost());
						for (InetAddress addr : addrs) {
							try {
								InetAddress tmp = InetAddress.getByAddress(addr.getAddress());
								hostnames.add(tmp.getHostName());
							} catch (UnknownHostException ex) {
							}
						}
					} catch (UnknownHostException ex) {
						// igoring
					}
				}

				for (String hostname : hostnames) {
					entries.add(new DnsEntry("http://" + hostname + ":5280/bosh", 0));
				}
			}

			if (!entries.isEmpty()) {
				return entries.toArray(new DnsEntry[entries.size()]);
			}
		}
		return null;
	}

	private static DnsEntry[] resolveWebSocket(DirContext ctx, String domain, DnsEntry[] c2sEntries) {
		try {
			Attributes attrs = ctx.getAttributes("_xmppconnect." + domain, new String[]{"TXT"});
			Attribute attr = attrs.get("TXT");

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
				if (!entries.isEmpty()) {
					return entries.toArray(new DnsEntry[entries.size()]);
				}
			}
		} catch (NamingException ex) {
			// no TXT record for domain
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
			if (!entries.isEmpty()) {
				return entries.toArray(new DnsEntry[entries.size()]);
			}
		}
		return null;
	}

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

	private static DnsEntry parseSrvEntry(String str) throws UnknownHostException {
		String[] parts = str.split(" ");
		int priority = 0;
		int weight = 0;
		int port = 5222;
		String host = parts[3];
		if (host.endsWith("."))
			host = host.substring(0, host.length()-1);
		
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

	public static void main(String[] argv) {
		get("hi-low.eu");
	}
	
	private static class ResolverTask implements Callable<DnsItem> {

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
