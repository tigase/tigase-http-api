package tigase.http.modules.setup.pages;

import gg.jte.output.StringOutput;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tigase.db.util.SchemaManager;
import tigase.http.modules.setup.NextPage;
import tigase.http.modules.setup.SetupModule;
import tigase.kernel.beans.Bean;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/databasePreparation")
@NextPage(SetupSecurityPage.class)
@Bean(name = "databasePreparationPage", parent = SetupModule.class, active = true)
public class DatabasePreparationPage
		extends AbstractPage {

	@Override
	public String getTitle() {
		return "Database preparation";
	}

	@GET
	public Response executeDbSchemaInstallation(HttpServletRequest request) {
		Map<SchemaManager.DataSourceInfo, List<SchemaManager.ResultEntry>> execResult = executeSchemaManager();
		StringOutput output = new StringOutput();
		Map<String, Object> context = prepareContext();

		List<SchemaManager.Pair<SchemaManager.DataSourceInfo, List<ResultEntry>>> result = execResult.entrySet()
				.stream()
				.map(e -> new SchemaManager.Pair<>(e.getKey(), e.getValue()
						.stream()
						.map(ResultEntry::new)
						.collect(Collectors.toList())))
				.collect(Collectors.toList());

		context.put("result", result);
		engine.render("dbPrepare.jte", context, output);
		return Response.ok(output.toString(), MediaType.TEXT_HTML).build();
	}

	@POST
	public Response processForm(HttpServletRequest request) {
		return redirectToNext(request);
	}
	
	public Map<SchemaManager.DataSourceInfo, List<SchemaManager.ResultEntry>> executeSchemaManager() {
		Map<String, Object> config = getConfig().getAsMap();

		SchemaManager schemaManager = new SchemaManager();
		schemaManager.setConfig(config);
		if (getConfig().getDbConfig().hasDbRootCredentials()) {
			schemaManager.setDbRootCredentials(getConfig().getDbConfig().getDbRootName(), getConfig().getDbConfig().getDbRootPassword());
		}
		if (!getConfig().getAdmins().isEmpty()) {
			schemaManager.setAdmins(getConfig().getAdmins().stream().toList(), getConfig().getAdminPwd());
		}

		return schemaManager.loadSchemas();
	}

	public static class ResultEntry {

		private final SchemaManager.ResultEntry entry;

		public ResultEntry(SchemaManager.ResultEntry entry) {
			this.entry = entry;
		}

		public String getName() {
			return entry.name;
		}

		public String getResultName() {
			return entry.result.name();
		}

		public String getMessage() {
			return switch (entry.result) {
				case ok -> null;
				default -> entry.message;
			};
		}

		public String getHeaderBackgroundClass() {
			return switch (entry.result) {
				case ok -> "bg-success";
				case skipped -> "bg-secondary";
				case warning -> "bg-warning";
				case error -> "bg-danger";
			};
		}

		public String getResultTextClass() {
			return switch (entry.result) {
				case ok -> "text-success";
				case skipped -> "text-secondary";
				case warning -> "text-warning";
				case error -> "text-danger";
			};
		}

	}

}
