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
package tigase.http.modules.setup.pages;

import gg.jte.output.StringOutput;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tigase.db.util.SchemaLoader;
import tigase.http.modules.setup.NextPage;
import tigase.http.modules.setup.SetupModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.xmpp.jid.BareJID;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/basic")
@NextPage(ConnectivityPage.class)
@Bean(name = "basicConfigPage", parent = SetupModule.class, active = true)
public class BasicConfigPage extends AbstractPage {

	@Override
	public String getTitle() {
		return "Basic Tigase server configuration";
	}

	@GET
	public Response getForm() {
		StringOutput output = new StringOutput();
		engine.render("basicConfig.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request, @FormParam("configType") ConfigTypeEnum configType,
								@FormParam("virtualDomain") String vhost, @FormParam("admins") String admins,
								@FormParam("adminPwd") String adminPwd, @FormParam("dbType") String dbTypeStr) {
		getConfig().setConfigType(Optional.ofNullable(configType).orElse(ConfigTypeEnum.DefaultMode));
		getConfig().setDefaultVirtualDomain(vhost);
		getConfig().setAdmins(Optional.ofNullable(admins)
									  .map(str -> str.split(","))
									  .stream()
									  .flatMap(Arrays::stream)
									  .map(BareJID::bareJIDInstanceNS)
									  .filter(Objects::nonNull)
									  .collect(Collectors.toSet()));
		getConfig().setAdminPwd(adminPwd);
		SchemaLoader.getAllSupportedTypesStream()
				.filter(type -> type.getName().equals(dbTypeStr))
				.findFirst()
				.ifPresent(type -> getConfig().setDbType(type));
		return redirectToNext(request);
	}

	public List<ConfigTypeEnum> getConfigTypes() {
		return List.of(ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode,
					   ConfigTypeEnum.ConnectionManagersMode, ConfigTypeEnum.ComponentMode);
	}

	public String getConfigTypeEnumLabel(ConfigTypeEnum value) {
		return switch (value) {
			case DefaultMode -> "Default installation";
			case SessionManagerMode -> "Session Manager only";
			case ConnectionManagersMode -> "Network connectivity only";
			case ComponentMode -> "External component only";
			default -> "";
		};
	}
}
