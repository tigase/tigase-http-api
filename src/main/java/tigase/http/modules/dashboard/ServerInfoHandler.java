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
package tigase.http.modules.dashboard;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tigase.http.jaxrs.Model;
import tigase.kernel.beans.Bean;
import tigase.server.XMPPServer;
import tigase.sys.TigaseRuntime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Bean(name = "serverInfo", parent = DashboardModule.class, active = true)
@Path("/server/info")
public class ServerInfoHandler extends DashboardHandler {

	@Override
	public Role getRequiredRole() {
		return Role.Admin;
	}

	@GET
	@Path("")
	@Produces({MediaType.TEXT_HTML})
	@RolesAllowed({"admin"})
	public Response info(Model model) {
		model.put("dataJson", prepareJSON());
		String output = renderTemplate("server/info.jte", model);
		return Response.ok(output, MediaType.TEXT_HTML).build();
	}

	@GET
	@Path("")
	@RolesAllowed({"admin"})
	@Produces({MediaType.APPLICATION_JSON})
	public Response infoJson() {
		return Response.ok(prepareJSON(), MediaType.APPLICATION_JSON).build();
	}

	private static StringBuilder append(StringBuilder sb, String name, String value) {
		sb.append("\"").append(name).append("\": ");
		sb.append("\"").append(value).append("\"");
		return sb;
	}

	private static StringBuilder append(StringBuilder sb, String name, int value) {
		sb.append("\"").append(name).append("\": ");
		sb.append(value);
		return sb;
	}

	private static StringBuilder append(StringBuilder sb, String name, double value) {
		sb.append("\"").append(name).append("\": ");
		sb.append(String.format(Locale.ROOT, "%.2f", value));
		return sb;
	}

	private String prepareJSON() {
		final TigaseRuntime runtime = TigaseRuntime.getTigaseRuntime();

		final StringBuilder sb = new StringBuilder();
		sb.append("{");

		append(sb, "data-uptime", runtime.getUptimeString()).append(",");
		append(sb, "data-load-average", runtime.getLoadAverage()).append(",");
		append(sb, "data-cpus-no", runtime.getCPUsNumber()).append(",");
		append(sb, "data-threads-count", runtime.getThreadsNumber()).append(",");

		append(sb, "data-cpu-usage-proc", runtime.getCPUUsage()).append(",");
		append(sb, "data-heap-usage-proc", runtime.getHeapMemUsage()).append(",");
		append(sb, "data-nonheap-usage-proc", runtime.getNonHeapMemUsage()).append(",");

		SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		append(sb, "version", XMPPServer.getImplementationVersion()).append(",");
		append(sb, "report-creation-timstamp", dtf.format(new Date()));
		sb.append("}");
		return sb.toString();
	}
}
