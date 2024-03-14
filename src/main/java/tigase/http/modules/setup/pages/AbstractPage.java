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

	@Override
	public Role getRequiredRole() {
		return Role.Admin;
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