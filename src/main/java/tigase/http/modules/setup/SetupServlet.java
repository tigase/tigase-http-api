package tigase.http.modules.setup;

import tigase.http.api.HttpException;
import tigase.http.jaxrs.JaxRsServlet;
import tigase.http.jaxrs.RequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SetupServlet extends JaxRsServlet<SetupHandler, SetupModule> {

	public SetupServlet() {
		
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ((req.getContextPath() + "/").equals(req.getRequestURI())) {
			String redirectTo = req.getRequestURL().toString() + module.getHandlers()
					.stream()
					.map(SetupHandler::getPath)
					.findFirst()
					.map(path -> path.substring(1))
					.get();
			resp.sendRedirect(redirectTo);
			return;
		}
		super.service(req, resp);
	}

	@Override
	protected void canAccess(RequestHandler<SetupHandler> requestHandler, HttpServletRequest request, HttpServletResponse response)
			throws HttpException, IOException, ServletException {
		if (!request.isUserInRole("admin") && !request.authenticate(response)) {
			request.authenticate(response);
			return;
		}
	}
	
}
