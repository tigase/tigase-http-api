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
package tigase.http.util;

import tigase.util.Algorithms;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;

public class AssetsServlet extends HttpServlet {

	private Path customAssetsPath;

	public AssetsServlet() {}

	@Override
	public void init() throws ServletException {
		super.init();
		Optional.ofNullable(getInitParameter("customAssetsPath"))
				.map(it -> "file:///" + it)
				.map(URI::create)
				.map(Paths::get)
				.ifPresent(it -> customAssetsPath = it);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp,true);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp, false);
	}

	private void processRequest(HttpServletRequest req, HttpServletResponse resp, boolean sendContent) throws IOException {
		String reqFile = req.getPathInfo();
		if (reqFile == null) {
			resp.sendError(SC_NOT_FOUND);
			return;
		}
		reqFile = URLDecoder.decode(reqFile, StandardCharsets.UTF_8);

		URL fileUrl = resolveFileUrl(reqFile);
		if (fileUrl == null) {
			resp.sendError(SC_NOT_FOUND);
			return;
		}

		String etag = null;
		URLConnection connection = fileUrl.openConnection();
		long lastModified = connection.getLastModified();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(("" + lastModified).getBytes(StandardCharsets.UTF_8));
			etag = Algorithms.bytesToHex(md.digest(reqFile.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			// can be ignored
		}

		String filename = reqFile;
		int idx = reqFile.lastIndexOf('/');
		if (idx >= 0) {
			filename = filename.substring(idx + 1);
		}

		String ifNoneMatch = req.getHeader("If-None-Match");
		if (ifNoneMatch != null && ifNoneMatch.equalsIgnoreCase(etag)) {
			resp.setStatus(SC_NOT_MODIFIED);
			resp.setHeader("ETag", etag);
			resp.setDateHeader("Expires", System.currentTimeMillis() + 60*1000);
			return;
		}
		
		long ifModifiedSince = req.getDateHeader("If-Modified-Since");
		if (ifNoneMatch == null && ifModifiedSince != 01 && ifModifiedSince + 1000 > lastModified) {
			resp.setStatus(SC_NOT_MODIFIED);
			resp.setHeader("ETag", etag);
			resp.setDateHeader("Expires", System.currentTimeMillis() + 60*1000);
			return;
		}

		resp.setDateHeader("Last-Modified", lastModified);

		String contentType = URLConnection.getFileNameMap().getContentTypeFor(filename);
		if (contentType == null) {
			if (filename.endsWith(".css")) {
				contentType = "text/css";
			} else {
				contentType = "application/octet-stream";
			}
		}
		resp.setHeader("ETag", etag);
		resp.setDateHeader("Expires", System.currentTimeMillis() + 60*1000);
		resp.setContentType(contentType);
		resp.setContentLength(connection.getContentLength());

		if (sendContent) {
			try (InputStream is = connection.getInputStream()) {
				is.transferTo(resp.getOutputStream());
			}
		}
	}

	private URL resolveFileUrl(String reqFile) throws MalformedURLException {
		if (customAssetsPath != null) {
			String filePath= reqFile;
			while (filePath.startsWith("/")) {
				filePath = filePath.substring(1);
			}
			Path assetsPath = customAssetsPath.resolve(filePath);
			File file = assetsPath.toFile();
			if (file.exists()) {
				return file.toURI().toURL();
			}
		}

		String path = "/tigase/assets" + reqFile;


		return AssetsServlet.class.getResource(path);
	}
}
