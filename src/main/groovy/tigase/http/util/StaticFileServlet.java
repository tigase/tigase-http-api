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
package tigase.http.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author andrzej
 */
public class StaticFileServlet extends HttpServlet {

	public static final String DIRECTORY_KEY = "directory";
	
	private File directory = null;
	
    @Override
    public void init() throws ServletException {
        super.init();
		ServletConfig cfg = super.getServletConfig();
		directory = new File(cfg.getInitParameter(DIRECTORY_KEY));		
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String prefix = req.getServletPath();
		prefix = req.getContextPath() + prefix;
		String pathStr = req.getRequestURI().replace(prefix, "");
		if (pathStr.contains("../")) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}
		
		Path path = Paths.get(directory.getAbsolutePath(), pathStr);
		if (!Files.exists(path)) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		String mimeType = Files.probeContentType(path);
		if (mimeType != null) {
			resp.setContentType(mimeType);
		}
		Files.copy(path, resp.getOutputStream());
	}

}
