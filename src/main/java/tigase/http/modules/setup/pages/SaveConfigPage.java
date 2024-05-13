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
import tigase.conf.ConfigHolder;
import tigase.http.api.HttpException;
import tigase.http.modules.setup.SetupModule;
import tigase.kernel.beans.Bean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Path("/saveConfig")
@Bean(name = "saveConfigPage", parent = SetupModule.class, active = true)
public class SaveConfigPage extends AbstractPage {

	private final static System.Logger logger = System.getLogger(SaveConfigPage.class.getName());

	@Override
	public String getTitle() {
		return "Saving configuration";
	}

	@GET
	public Response getConfigForm() {
		StringOutput output = new StringOutput();
		engine.render("saveConfig.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request, @FormParam("config") String config) throws HttpException {
		logger.log(System.Logger.Level.TRACE, "Saving config: \n" + config );
		var configPath = Paths.get(ConfigHolder.TDSL_CONFIG_FILE_DEF);
		try {
			File f = configPath.toFile();
			if (configPath.toFile().exists()) {
				ConfigHolder.backupOldConfigFile(configPath);
			} else {
				f.createNewFile();
			}
			Files.writeString(configPath, config);
		} catch (IOException ex) {
			throw new HttpException(ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}


		StringOutput output = new StringOutput();
		engine.render("finished.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}
}