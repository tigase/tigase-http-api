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
package tigase.http.ui;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class implements support for deploying static content in form of a compressed
 * archive in WAR format.
 * 
 * @author andrzej
 */
public class WarServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(WarServlet.class.getCanonicalName());
	
	public static final String WAR_PATH_KEY = "war-path";
	private ZipFile war;
	
    @Override
    public void init() throws ServletException {
        super.init();
		ServletConfig cfg = super.getServletConfig();
		String warPath = cfg.getInitParameter(WAR_PATH_KEY);
		try {
			war = new ZipFile(warPath);
		} catch (IOException ex) {
			log.log(Level.FINE, "Could not initialize servlet, wrong path " + warPath + " to WAR archive?", ex);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		URI uri = URI.create(req.getRequestURI());
		String path = uri.getPath();
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "got request for {0} and servlet path {1} context {2}", 
					new Object[]{path, req.getServletPath(), req.getContextPath()});
		}
		if (path.startsWith(req.getContextPath()))
			path = path.substring(req.getContextPath().length());
		if (path.startsWith(req.getServletPath()))
			path = path.substring(req.getServletPath().length());
		if (path.isEmpty() || path.equals("/"))
			path = "index.html";
		while (path.startsWith("/"))
			path = path.substring(1);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "converted to request for file from relative path {0}", path);
		}
		ZipEntry e = war.getEntry(path);
		if (e == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "404 Not found");
			return;
		}
		byte[] buf = new byte[4096];
		try (InputStream in = war.getInputStream(e)) {
			OutputStream out = resp.getOutputStream();
			
			int read;
			while ((read = in.read(buf)) != -1) {
				out.write(buf, 0, read);
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (war != null) {
			war.close();
		}
		super.finalize();
	}

}
