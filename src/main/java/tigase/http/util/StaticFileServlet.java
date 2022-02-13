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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * @author andrzej
 */
public class StaticFileServlet
		extends HttpServlet {

	public static final String ALLOWED_PATTERN_KEY = "allowed-pattern";
	public static final String DIRECTORY_KEY = "directory";
	public static final String INDEX_KEY = "index";

	private Pattern allowedPattern = null;
	private File directory = null;
	private String index = null;

	@Override
	public void init() throws ServletException {
		super.init();
		ServletConfig cfg = super.getServletConfig();
		directory = new File(cfg.getInitParameter(DIRECTORY_KEY));
		index = cfg.getInitParameter(INDEX_KEY);
		String allowedPatternStr = cfg.getInitParameter(ALLOWED_PATTERN_KEY);
		if (allowedPatternStr != null) {
			allowedPattern = Pattern.compile(allowedPatternStr);
		}
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
		if ((pathStr == null || pathStr.isEmpty() || pathStr.equals("/")) && index != null) {
			pathStr = index;
		}

		if (allowedPattern != null && !allowedPattern.matcher(pathStr).matches()) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
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
