@import jakarta.ws.rs.core.SecurityContext;
@import jakarta.ws.rs.core.UriInfo
@import jakarta.servlet.http.HttpServletRequest
@param SecurityContext securityContext
@param UriInfo uriInfo
@param HttpServletRequest request

@template.layout(securityContext = securityContext, uriInfo = uriInfo, request = request, content = @`
    <div class="text-center py-4 mx-auto w-75">
        <h1 class="text-secondary">Dashboard</h1>
        <h4 class="text-secondary">Welcome to Tigase XMPP Server dashboard.</h4>
        <p class="text-secondary small py-5">Main configuration categories are located on the left sidebar.<br/>After selecting each of them, values and operations related to each category will appear.</p>
    </div>
`)
