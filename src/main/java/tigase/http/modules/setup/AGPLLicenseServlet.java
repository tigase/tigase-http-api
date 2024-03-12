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
package tigase.http.modules.setup;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AGPLLicenseServlet
		extends HttpServlet {

	private static final Logger log = Logger.getLogger(AGPLLicenseServlet.class.getCanonicalName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		File licenceFile = new File("License.html");
		if (!licenceFile.exists()) {
			resp.setStatus(404, "File is missing");
			return;
		}
		resp.setContentType("text/html");
		resp.setContentLength((int) licenceFile.length());

		try (ReadableByteChannel inChannel = Channels.newChannel(new FileInputStream(licenceFile))) {
			WritableByteChannel outChannel = Channels.newChannel(resp.getOutputStream());
			resp.setStatus(200);
			transferData(inChannel, outChannel);
		} catch (IOException ex) {
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
}
