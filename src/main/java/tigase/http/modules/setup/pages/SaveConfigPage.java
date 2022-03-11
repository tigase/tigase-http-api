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

@Path("/saveConfig")
@Bean(name = "saveConfigPage", parent = SetupModule.class, active = true)
public class SaveConfigPage extends AbstractPage {

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
		try {
			File f = new File(ConfigHolder.TDSL_CONFIG_FILE_DEF);
			if (f.exists()) {
				File bf = new File(ConfigHolder.TDSL_CONFIG_FILE_DEF);
				if (!bf.exists()) {
					bf.createNewFile();
				}
				try (FileOutputStream fos = new FileOutputStream(bf, false)) {
					try (FileInputStream fis = new FileInputStream(f)) {
						byte[] tmp = new byte[1024];
						int read = 0;
						while ((read = fis.read(tmp)) > -1) {
							fos.write(tmp, 0, read);
						}
					}
				}
			} else {
				f.createNewFile();
			}
		} catch (IOException ex) {
			throw new HttpException(ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		StringOutput output = new StringOutput();
		engine.render("finished.jte", prepareContext(), output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}
}
