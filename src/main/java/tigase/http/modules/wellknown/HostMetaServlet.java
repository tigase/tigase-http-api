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
package tigase.http.modules.wellknown;

import tigase.http.modules.AbstractModule;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HostMetaServlet extends HttpServlet {

	public static final String MODULE_ID_KEY = "module-id-key";

	private WellKnownModule module;

	@Override
	public void init() throws ServletException {
		super.init();
		ServletConfig cfg = super.getServletConfig();
		String moduleName = cfg.getInitParameter(MODULE_ID_KEY);
		module = AbstractModule.getModuleByUUID(moduleName);
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Short description";
	}// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 *
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Format format = Optional.ofNullable(getInitParameter("format"))
				.map(Format::valueOf)
				.or(() -> Optional.ofNullable(request.getPathInfo())
						.map(path -> path.startsWith("/host-meta.json"))
						.map(v -> v ? Format.json : Format.xml))
				.orElse(Format.xml);

		String domain = Optional.ofNullable(request.getHeader("domain"))
				.orElse(Optional.of(request.getServerName()).map(str -> str.split(":")).map(arr -> arr[0]).get());
		PrintWriter out = null;

		try {
			List<Link> links = module.getHostMetaLinks(domain);
			if (links == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} else {
				response.setHeader("Access-Control-Allow-Origin", "*");
				StringBuilder sb = new StringBuilder(1000);
				switch (format) {
					case xml:
						response.setContentType("application/xml");
						out = response.getWriter();
						sb.append("<?xml version='1.0' encoding='utf-8'?>\n" +
										  "<XRD xmlns='http://docs.oasis-open.org/ns/xri/xrd-1.0'>");
						sb.append(links.stream().map(Link::toXML).collect(Collectors.joining()));
						sb.append("</XRD>");
						break;
					case json:
						response.setContentType("application/json");
						out = response.getWriter();
						String callback = request.getParameter("callback");
						if (callback != null) {
							sb.append(callback);
							sb.append("(");
						}

						sb.append("{ \"links\": [");

						sb.append(links.stream().map(Link::toJSON).collect(Collectors.joining(",")));
						sb.append("]}");

						if (callback != null) {
							sb.append(")");
						}
						break;
				}
				out.append(sb.toString());
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 *
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 *
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	interface Link {
		String getType();
		String getURL();

		String toJSON();
		String toXML();
	}

	public static class AbstractLink implements Link {
		final String type;
		final String url;
		AbstractLink(String type, String url) {
			this.type = type;
			this.url = url;
		}

		@Override
		public String getURL() {
			return url;
		}

		@Override
		public String getType() {
			return type;
		}

		public String toJSON() {
			return "{\"rel\": \"" + getType() + "\", \"href\": \"" + getURL() + "\"}";
		}

		public String toXML() {
			return "<Link rel=\"" + getType() + "\" href=\"" + getURL() + "\" />";
		}
	}

	public static class WebSocketLink extends AbstractLink {
		public WebSocketLink(String url) {
			super("urn:xmpp:alt-connections:websocket", url);
		}
	}

	public static class BoshLink extends AbstractLink {
		public BoshLink(String url) {
			super("urn:xmpp:alt-connections:xbosh", url);
		}
	}

	enum Format {
		json,
		xml
	}

}
