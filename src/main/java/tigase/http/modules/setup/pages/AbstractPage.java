package tigase.http.modules.setup.pages;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import tigase.http.modules.setup.Config;
import tigase.http.modules.setup.NextPage;
import tigase.http.modules.setup.SetupHandler;
import tigase.http.modules.setup.SetupModule;
import tigase.kernel.beans.Inject;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPage implements SetupHandler {

	protected final TemplateEngine engine;
	@Inject(nullAllowed = true)
	protected SetupModule setupModule;

	public AbstractPage() {
		this.engine = TemplateEngine.create(new ResourceCodeResolver("tigase/setup"), ContentType.Html);
	}

	public Config getConfig() {
		return setupModule.getConfig();
	}

	public String getPath() {
		return getClass().getAnnotation(Path.class).value();
	}

	public Map<String,Object> prepareContext() {
		Map<String,Object> context = new HashMap<>();
		context.put("pages", setupModule.getHandlers());
		context.put("currentPage", this);
		context.put("config", getConfig());
		return context;
	}

	protected String getNextPagePath(HttpServletRequest request) {
		Class<? extends SetupHandler> nextClass = getClass().getAnnotation(NextPage.class).value();
		SetupHandler nextHandler = setupModule.getHandlers().stream().filter(nextClass::isInstance).findFirst().get();
		return request.getRequestURL().toString().replace(getPath(), nextHandler.getPath());
	}

	protected Response redirectToNext(HttpServletRequest request) {
		String uri = getNextPagePath(request);
		return Response.seeOther(URI.create(uri)).build();
	}
}
