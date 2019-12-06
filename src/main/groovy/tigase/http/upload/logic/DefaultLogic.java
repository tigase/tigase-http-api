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

import tigase.component.exceptions.ComponentException;
import tigase.db.TigaseDBException;
import tigase.http.api.HttpServerIfc;
import tigase.http.upload.FileUploadComponent;
import tigase.http.upload.db.FileUploadRepository;
import tigase.http.upload.store.Store;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.dns.DNSResolverFactory;
import tigase.vhosts.VHostManager;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by andrzej on 07.08.2016.
 */
@Bean(name = "logic", parent = FileUploadComponent.class, active = true, exportable = true)
public class DefaultLogic
		implements Logic {

	private static final Logger log = Logger.getLogger(DefaultLogic.class.getCanonicalName());

	public static enum HttpProtocol {
		http,
		https
	}
	@ConfigField(desc = "Download URI format", alias = "download-uri-format")
	private UriFormat downloadUriFormat = new UriFormat("{proto}://{serverName}:{port}/upload/{slotId}/{filename}");
	@ConfigField(desc = "Remove old uploads - expiration time", alias = "expiration")
	private Duration expiration = Duration.ofDays(30);
	@ConfigField(desc = "Remove old uploads - period", alias = "expiration-period")
	private Duration expirationPeriod = Duration.ZERO;
	@Inject
	private HttpServerIfc httpServer;
	@ConfigField(desc = "Allow file upload for local clients", alias = "local-only")
	private boolean localOnly = true;
	@ConfigField(desc = "Maximal file size allowed for transfer", alias = "max-file-size")
	private long maxFileSize = 5 * 1024 * 1024;
	@ConfigField(desc = "Port")
	private Integer port = null;
	@ConfigField(desc = "Protocol")
	private HttpProtocol protocol = null;
	@Inject
	private FileUploadRepository repo;
	@ConfigField(desc = "HTTP server domain name", alias = "server-name")
	private String serverName = DNSResolverFactory.getInstance().getDefaultHost();
	@Inject
	private Store store;
	@ConfigField(desc = "Upload URI format", alias = "upload-uri-format")
	private UriFormat uploadUriFormat = new UriFormat(
			"{proto}://{serverName}:{port}/upload/{userJid}/{slotId}/{filename}");
	@Inject
	private VHostManager vHostManager;

	public String getUploadUriFormat() {
		return this.uploadUriFormat.getFormat();
	}

	public void setUploadUriFormat(String format) {
		this.uploadUriFormat = new UriFormat(format);
	}

	public String getDownloadUriFormat() {
		return this.downloadUriFormat.getFormat();
	}

	public void setDownloadUriFormat(String format) {
		this.downloadUriFormat = new UriFormat(format);
	}

	@Override
	public long getMaxFileSize() {
		return maxFileSize;
	}

	@Override
	public String requestSlot(JID requester, String filename, long filesize, String contentType)
			throws ComponentException {
		if (localOnly && !vHostManager.isLocalDomain(requester.getDomain())) {
			throw new ComponentException(Authorization.NOT_ALLOWED, "Only local XMPP users may use this service");
		}

		if (maxFileSize < filesize) {
			throw new ComponentException(Authorization.NOT_ACCEPTABLE,
										 "File too large. The maximum allowed file size is " + maxFileSize + " bytes");
		}

		String slotId = generateSlotId();

		try {
			FileUploadRepository.Slot slot = repo.allocateSlot(requester, slotId, filename, filesize, contentType);
			return slot == null ? null : slot.slotId;
		} catch (TigaseDBException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public String getUploadURI(JID requester, String slotId, String filename) {
		return generateURI(uploadUriFormat, requester, slotId, filename);
	}

	@Override
	public String getDownloadURI(JID requester, String slotId, String filename) {
		return generateURI(downloadUriFormat, requester, slotId, filename);
	}

	@Override
	public String generateSlotId() {
		return UUID.randomUUID().toString();
	}

	@Override
	public UriFormat getUploadURIFormat() {
		return uploadUriFormat;
	}

	@Override
	public UriFormat getDownloadURIFormat() {
		return downloadUriFormat;
	}

	@Override
	public void removeExpired(Duration expirationTime, int limit) {
		LocalDateTime expiredBefore = LocalDateTime.now(ZoneId.of("Z")).minus(expirationTime);
		for (JID vhost : vHostManager.getAllVHosts()) {
			try {
				List<FileUploadRepository.Slot> removedSlots = repo.listExpiredSlots(vhost.getBareJID(), expiredBefore,
																					 limit);
				for (FileUploadRepository.Slot slot : removedSlots) {
					store.remove(slot.uploader, slot.slotId);
				}
				repo.removeExpiredSlots(vhost.getBareJID(), expiredBefore, limit);
			} catch (TigaseDBException ex) {
				log.log(Level.FINE, "retrieval of expired slots failed", ex);
			} catch (IOException ex) {
				log.log(Level.FINE, "removal of slot failed", ex);
			}
		}
	}

	protected String generateURI(UriFormat format, JID requester, String slotId, String filename) {
		Integer port = this.port;
		HttpProtocol protocol = this.protocol;
		if (port == null || protocol == null) {
			List<Integer> ports = httpServer.getHTTPSPorts();
			if (ports != null && !ports.isEmpty()) {
				port = ports.get(0);
				protocol = HttpProtocol.https;
			} else {
				ports = httpServer.getHTTPPorts();
				if (ports != null && !ports.isEmpty()) {
					port = ports.get(0);
					protocol = HttpProtocol.http;
				}
			}

		}

		if (port == null || protocol == null) {
			throw new RuntimeException(
					"Could not detect port and schema to use - possible misconfiguration of HTTP server");
		}

		return format.formatUri(protocol, serverName, port, requester, slotId, filename);
	}

	private Pattern compileToUriMatcher(String format) {
		int jidIdx = format.indexOf("{userJid}");
		int slotIdx = format.indexOf("{slotId}");
		int filenameIdx = format.indexOf("{filename}");

		int idx = Math.min(slotIdx, filenameIdx);
		if (jidIdx > -1) {
			idx = Math.min(idx, jidIdx);
		}

		String infoTemp = format.substring(idx).replace("/", "\\/");
		infoTemp = infoTemp.replace("{userJid}", "(?<jid>[^/]+)")
				.replace("{slotId}", "(?<slotId>[^/]+)")
				.replace("{filename}", "(?<filename>[^/]+)");

		return Pattern.compile(infoTemp);
	}

}
