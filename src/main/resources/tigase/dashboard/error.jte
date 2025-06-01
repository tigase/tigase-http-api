@import jakarta.ws.rs.core.SecurityContext
@import jakarta.ws.rs.core.UriInfo
@import javax.servlet.http.HttpServletRequest
@param SecurityContext securityContext
@param UriInfo uriInfo
@param HttpServletRequest request
@param int code
@param String message
@param String reason = null
@param Throwable exception = null
@param String stacktrace = null

@template.layout(securityContext = securityContext, uriInfo = uriInfo, request = request, content = @`
    <div class="p-4">
    @if (code == 404 || code == 403 || code == 400)
        <div class="alert alert-secondary container" role="alert">
            <div class="row">
                <h1>${code} ${message}</h1>
            </div>
            <div class="row mt-4">
                <div class="col-sm">
                    <a class="btn btn-lg btn-primary" type="button" onclick="window.history.back()">Back</a>
                </div>
            </div>
        </div>
    @else
        <div class="alert alert-danger container" role="alert">
            <div class="row">
                <h1>${code} ${message}</h1>
            </div>
            @if (reason != null)
            <div class="row">
                <h3>${reason}</h3>
            </div>
            @endif
            <div class="row mt-4">
                <div class="col-sm">
                    <a class="btn btn-lg btn-primary" type="button" onclick="window.history.back()">Back</a>
                </div>
                @if (stacktrace != null)
                <div class="col-sm d-flex justify-content-end align-items-end">
                    <button class="btn btn-secondary btn-sm" type="button" data-bs-toggle="collapse" data-bs-target="#error-details" aria-expanded="false" aria-controls="error-details">
                        Show details
                    </button>
                </div>
                @endif
            </div>
            @if (stacktrace != null)
                <div class="row" >
                    <div class="collapse" id="error-details">
                        <br>
<%--                        <div class="alert alert-light text-wrap">--%>
<%--                            $unsafe{stacktrace.replaceAll("\n", "<br/>").replaceAll("\t","&nbsp;&nbsp;&nbsp;&nbsp;")}--%>
<%--                        </div>--%>
                        <div class="alert alert-light text-wrap copyable">
                            <pre>${stacktrace}</pre>
                        </div>
                    </div>
                </div>
            @endif
        </div>
    @endif
    </div>
`)
