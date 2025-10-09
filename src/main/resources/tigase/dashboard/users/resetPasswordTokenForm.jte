@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.UserPasswordRecovery
@import tigase.server.XMPPServer
@import java.util.List
@param UriInfo uriInfo
@param List<String> errors
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/html">
<head>
    <style>
        body {
            font-family: var(--bs-body-font-family);
        }
        .footer {
            font-size: inherit;
        }
        img.logo {
            content: url(${uriInfo.getBaseUriBuilder().path("/assets/images/logo.png").build().getPath()});
        }
    </style>
    @template.head-common(uriInfo = uriInfo)
    <script type="javascript" src="assets/js/bootstrap.bundle.min.js"></script>
</head>
<body class="d-flex flex-column bg-light vh-100 overflow-auto">
<%--@if(error != null)--%>
<%--    <div class="alert alert-danger text-center" role="alert">${error}</div>--%>
<%--@endif--%>
<main class="text-center m-auto" style="height: unset">
    <form style="width: 450px" method="post">
        <img class="logo mb-4" style="width: 170px">
        <h1 class="h3 mb-3 fw-normal">Tigase XMPP Server</h1>
        <h5 class="h5 mb-3 fw-lighter text-secondary">Reset your password.</h5>
        @if (errors != null && !errors.isEmpty())
            <div class="text-danger h5 fw-lighter">
                @for (String error : errors)
                    <p>${error}</p>
                @endfor
            </div>
        @endif
        <div class="form-floating mb-3">
            <input type="text" class="form-control" placeholder="New password" id="password-entry" name="password-entry" required>
            <label for="password-entry">New password</label>
        </div>
        <div class="form-floating mb-3">
            <input type="text" class="form-control" placeholder="Reenter password" id="password-reentry" name="password-reentry" required>
            <label for="password-reentry">Reenter password</label>
        </div>
        <button type="submit" class="w-100 btn btn-lg btn-primary mb-2">Reset password</button>
    </form>
</main>
<footer class="footer">
    <div class="small text-muted text-end m-1">Tigase XMPP Server ${XMPPServer.getImplementationVersion()}</div>
</footer>
</body>
</html>