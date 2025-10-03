@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.UserRegisterHandler
@import tigase.server.XMPPServer
@import java.util.List
@param UriInfo uriInfo
@param List<String> domains
@param boolean emailRequired
@param String captcha
@param String captchaID
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
    <form style="width: 450px" method="post" action="${uriInfo.getBaseUriBuilder().path(UserRegisterHandler.class, "register").build().getPath()}">
        <img class="logo mb-4" style="width: 170px">
        <h1 class="h3 mb-3 fw-normal">Tigase XMPP Server</h1>
        <h5 class="h5 mb-3 fw-lighter text-secondary">Please provide the following information to sign up for an account.</h5>
        <div class="input-group mb-3">
            <div class="form-floating flex-grow-1">
                <input type="text" class="form-control" placeholder="Username" id="username" name="username" required>
                <label for="username">Username</label>
            </div>
            <span class="input-group-text">@</span>
            <div class="form-floating">
                <select class="form-select" id="domain" name="domain" required>
                    @for(String domain : domains)
                    <option value="${domain}">${domain}</option>
                    @endfor
                </select>
                <label for="domain">Domain</label>
            </div>
        </div>
        <div class="form-floating mb-3">
            <input id="password" type="password" name="password" placeholder="*****" class="form-control" required>
            <label for="password">Password</label>
        </div>
        <div class="form-floating mb-3">
            <input id="email" type="email" name="email" placeholder="Email" class="form-control" required="${emailRequired}">
            <label for="email">Email</label>
        </div>
        @if (captcha != null)
        <div class="form-floating mb-3">
            <input id="captcha" type="text" name="captcha" placeholder="Result" class="form-control" required>
            <label for="captcha">${captcha}</label>
        </div>
        <input type="hidden" id="captchaID" name="captchaID" value="${captchaID}">
        @endif
        <button type="submit" class="w-100 btn btn-lg btn-primary mb-2">Register</button>
    </form>
</main>
<footer class="footer">
    <div class="small text-muted text-end m-1">Tigase XMPP Server ${XMPPServer.getImplementationVersion()}</div>
</footer>
</body>
</html>