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
package tigase.http.upload;

import tigase.http.AbstractHttpModule;
import tigase.http.upload.db.FileUploadRepository;
import tigase.http.upload.logic.Logic;
import tigase.http.upload.logic.UriFormat;
import tigase.http.upload.store.Store;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.xmpp.jid.BareJID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Created by andrzej on 08.08.2016.
 */
public class FileServlet
		extends HttpServlet {

	private static final Logger log = Logger.getLogger(FileServlet.class.getCanonicalName());

	private FileServletContext context;

	@Override
	public void init() throws ServletException {
		super.init();

		ServletConfig cfg = super.getServletConfig();
		String kernelId = cfg.getInitParameter("kernel");

		Kernel kernel = AbstractHttpModule.getKernel(kernelId);

		context = kernel.getInstance(FileServletContext.class);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setHeader("Access-Control-Allow-Origin", "*");
			UriFormat uriFormat = context.logic.getDownloadURIFormat();

			Matcher m = uriFormat.parsePath(req.getPathInfo().substring(1));

			if (!m.matches()) {
				resp.sendError(404);
				return;
			}

			BareJID uploader = null;
			if (uriFormat.hasGroup("jid")) {
				String jidStr = m.group("jid");
				uploader = jidStr.isEmpty() ? null : BareJID.bareJIDInstanceNS(jidStr);
			} else if (uriFormat.hasGroup("domain")) {
				String domain = m.group("domain");
				uploader = domain.isEmpty() ? null : BareJID.bareJIDInstanceNS(domain);
			}

			String slotId = m.group("slotId");
			String filename = m.group("filename");

			FileUploadRepository.Slot slot = context.repo.getSlot(uploader, slotId);
			if (slot == null) {
				resp.sendError(404);
				return;
			}

			WritableByteChannel outChannel = Channels.newChannel(resp.getOutputStream());
			try (ReadableByteChannel inChannel = context.store.getContent(slot.uploader, slot.slotId, filename)) {
				if (inChannel == null) {
					resp.sendError(404);
				} else {
					resp.setHeader("Content-Length", String.valueOf(slot.filesize));
					resp.setHeader("Content-Disposition", "attachment; filename=\"" + slot.filename + "\"");
					if (slot.contentType != null) {
						resp.setHeader("Content-Type", slot.contentType);
					}
					resp.setStatus(200);
					transferData(inChannel, outChannel);
				}
			} catch (Exception ex) {
				resp.sendError(500, "Internal Server Error");
				log.log(Level.FINE, "Exception during processing request", ex);
			}
		} catch (Throwable ex) {
			resp.sendError(500, "Internal Server Error");
			log.log(Level.FINE, "Exception during processing request", ex);
		}
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setHeader("Access-Control-Allow-Origin", "*");
			UriFormat uriFormat = context.logic.getDownloadURIFormat();

			Matcher m = uriFormat.parsePath(req.getPathInfo().substring(1));

			if (!m.matches()) {
				resp.sendError(404);
				return;
			}

			BareJID uploader = null;
			if (uriFormat.hasGroup("jid")) {
				String jidStr = m.group("jid");
				uploader = jidStr.isEmpty() ? null : BareJID.bareJIDInstanceNS(jidStr);
			} else if (uriFormat.hasGroup("domain")) {
				String domain = m.group("domain");
				uploader = domain.isEmpty() ? null : BareJID.bareJIDInstanceNS(domain);
			}

			String slotId = m.group("slotId");
			String filename = m.group("filename");

			FileUploadRepository.Slot slot = context.repo.getSlot(uploader, slotId);
			if (slot == null) {
				resp.sendError(404);
				return;
			}

			resp.setHeader("Content-Length", String.valueOf(slot.filesize));
			if (slot.contentType != null) {
				resp.setHeader("Content-Type", slot.contentType);
			}

			resp.setStatus(200);
		} catch (Throwable ex) {
			resp.sendError(500, "Internal Server Error");
			log.log(Level.FINE, "Exception during processing request", ex);
		}
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doOptions(req, resp);
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Methods", "PUT, GET, OPTIONS");
		resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
		resp.setHeader("Access-Control-Max-Age", "86400");
		resp.setStatus(200);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setHeader("Access-Control-Allow-Origin", "*");
			UriFormat uriFormat = context.logic.getUploadURIFormat();

			Matcher m = uriFormat.parsePath(req.getPathInfo().substring(1));

			if (!m.matches()) {
				resp.sendError(404);
				return;
			}

			BareJID uploader = null;
			if (uriFormat.hasGroup("jid")) {
				String jidStr = m.group("jid");
				uploader = jidStr.isEmpty() ? null : BareJID.bareJIDInstance(jidStr);
			} else if (uriFormat.hasGroup("domain")) {
				String domain = m.group("domain");
				uploader = domain.isEmpty() ? null : BareJID.bareJIDInstance(domain);
			}

			String slotId = m.group("slotId");
			String filename = m.group("filename");

			String contentType = req.getContentType();
			long fileSize = req.getContentLengthLong();

			FileUploadRepository.Slot slot = context.repo.getSlot(uploader, slotId);

			if (slot != null && slot.matches(slotId, fileSize, contentType)) {
				ReadableByteChannel inChannel = Channels.newChannel(req.getInputStream());
				context.store.setContent(uploader, slotId, filename, fileSize, inChannel);

				context.repo.updateSlot(slot.uploader, slotId);
				resp.setStatus(201);
			} else {
				resp.sendError(404);
			}
		} catch (Throwable ex) {
			resp.sendError(500, "Internal Server Error");
			log.log(Level.FINE, "Exception during processing request", ex);
		}
	}

	protected void transferData(ReadableByteChannel in, WritableByteChannel out) throws IOException {
		if (in instanceof FileChannel) {
			FileChannel fin = (FileChannel) in;
			long size = fin.size();
			fin.transferTo(0, size, out);
		} else {
			ByteBuffer buf = ByteBuffer.allocate(64 * 1024);
			while (in.read(buf) != -1) {
				buf.flip();
				out.write(buf);
				buf.compact();
			}
		}
	}

	@Bean(name = "fileServletContext", parent = HttpModule.class, active = true)
	public static class FileServletContext {

		@Inject
		public Logic logic;

		@Inject
		public FileUploadRepository repo;

		@Inject
		public Store store;

	}
}
