@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.UserPasswordRecovery
@import tigase.server.XMPPServer
@import java.util.List
@param UriInfo uriInfo
@param String captcha
@param String captchaID
@param Boolean enabled
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
    <form style="width: 450px" method="post" action="${uriInfo.getBaseUriBuilder().path(UserPasswordRecovery.class).build().getPath()}">
        <img class="logo mb-4" style="width: 170px">
        <h1 class="h3 mb-3 fw-normal">Tigase XMPP Server</h1>
        <h5 class="h5 mb-3 fw-lighter text-secondary">Please fill out this form to reset password for an account.</h5>
        @if (errors != null && !errors.isEmpty())
            <div class="text-danger h5 fw-lighter">
            @for (String error : errors)
                <p>${error}</p>
            @endfor
            </div>
        @endif
        <div class="form-floating mb-3">
            <input type="text" class="form-control" placeholder="JID" id="jid" name="jid" required>
            <label for="jid">XMPP address (JID)</label>
        </div>
        <div class="form-floating mb-3">
            <input id="email" type="email" name="email" placeholder="Email" class="form-control" required>
            <label for="email">Email</label>
            <small class="text-muted">Email used during registration</small>
        </div>
        @if (captcha != null)
        <div class="form-floating mb-3">
            <input id="captcha" type="text" name="captcha" placeholder="Result" class="form-control" required>
            <label for="captcha">${captcha}</label>
        </div>
        <input type="hidden" id="captchaID" name="captchaID" value="${captchaID}">
        @endif
        <button type="submit" class="w-100 btn btn-lg btn-primary mb-2" disabled="${enabled != null && !enabled}">Reset password</button>
    </form>
</main>
<footer class="footer">
    <div class="small text-muted text-end m-1">Tigase XMPP Server ${XMPPServer.getImplementationVersion()}</div>
</footer>
</body>
</html>