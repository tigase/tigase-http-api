package tigase.http.jaxrs;

import jakarta.ws.rs.core.SecurityContext;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

public class SecurityContextImpl implements SecurityContext {

	private HttpServletRequest request;

	public SecurityContextImpl(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public Principal getUserPrincipal() {
		return request.getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String s) {
		return request.isUserInRole(s);
	}

	@Override
	public boolean isSecure() {
		return "https".equalsIgnoreCase(request.getProtocol());
	}

	@Override
	public String getAuthenticationScheme() {
		return request.getAuthType();
	}
}
