/*
 * DnsHostMetaServlet.java
 *
 * Tigase HTTP API
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DnsHostMetaServlet extends HttpServlet {

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
		String path = request.getPathInfo();
		if (!path.startsWith("/host-meta")) {
			response.sendError(404, "Not found");
			return;
		}
		Format format = path.startsWith("/host-meta.json") ? Format.json : Format.xml;
		String domain = Optional.ofNullable(request.getHeader("domain"))
				.orElse(Optional.of(request.getServerName()).map(str -> str.split(":")).map(arr -> arr[0]).get());
		PrintWriter out = null;

		try {
			DnsItem item = DnsResolver.get(domain);
			if (item == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} else {
				response.setHeader("Access-Control-Allow-Origin", "*");
				StringBuilder sb = new StringBuilder(1000);
				Stream<Link> urlsStream = Stream.concat(Optional.ofNullable(item.getWebSocket())
																.map(Arrays::stream)
																.orElse(Stream.empty())
																.map(DnsEntry::getURL)
																.filter(Objects::nonNull)
																.map(WebSocketLink::new),
														Optional.ofNullable(item.getBosh())
																.map(Arrays::stream)
																.orElse(Stream.empty())
																.map(DnsEntry::getURL)
																.filter(Objects::nonNull)
																.map(BoshLink::new));
				switch (format) {
					case xml:
						response.setContentType("application/xml");
						out = response.getWriter();
						sb.append("<?xml version='1.0' encoding='utf-8'?>\n" +
										  "<XRD xmlns='http://docs.oasis-open.org/ns/xri/xrd-1.0'>");
						sb.append(urlsStream.map(Link::toXML).collect(Collectors.joining()));
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

						sb.append(urlsStream.map(Link::toJSON).collect(Collectors.joining(",")));
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

	class AbstractLink implements Link {
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

	class WebSocketLink extends AbstractLink  {
		WebSocketLink(String url) {
			super("urn:xmpp:alt-connections:websocket", url);
		}
	}

	class BoshLink extends AbstractLink {
		BoshLink(String url) {
			super("urn:xmpp:alt-connections:xbosh", url);
		}
	}

	enum Format {
		json,
		xml
	}
}
