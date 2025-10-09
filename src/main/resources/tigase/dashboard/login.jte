@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.UserPasswordRecovery
@import tigase.http.modules.dashboard.UserRegisterHandler
@import tigase.server.XMPPServer
@param UriInfo uriInfo
@param String error = null
@param boolean registrationEnabled
@param boolean passwordResetEnabled
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
@if(error != null)
<div class="alert alert-danger text-center" role="alert">${error}</div>
@endif
<main class="text-center m-auto" style="height: unset">
    <form style="width: 300px" method="post" action="${uriInfo.getBaseUriBuilder().path("/login").build().getPath()}">
        <img class="logo mb-4" style="width: 170px">
        <h1 class="h3 mb-3 fw-normal">Tigase XMPP Server</h1>
        <div class="form-floating mb-3">
            <input id="jid" type="email" name="jid" placeholder="user@domain.com" class="form-control" required>
            <label for="jid">JID</label>
        </div>
        <div class="form-floating mb-3">
            <input id="password" type="password" name="password" placeholder="*****" class="form-control" required>
            <label for="password">Password</label>
        </div>
        <button type="submit" class="w-100 btn btn-lg btn-primary"><i class="bi bi-key me-2"></i>Sign in</button>
        @if (passwordResetEnabled)
        <div class="mt-2">
        <a class="text-decoration-none text-secondary" style="font-size: 0.9em; font-weight: 500;" href="${uriInfo.getBaseUriBuilder().path(UserPasswordRecovery.class).build().getPath()}">Forgotten password?</a>
        </div>
        @endif
        @if (registrationEnabled)
        <div class="w-100 text-center text-secondary fw-bolder my-3">or</div>
        <a class="btn w-100 btn-outline-secondary" href="${uriInfo.getBaseUriBuilder().path(UserRegisterHandler.class, "register").build().getPath()}"><i class="bi bi-plus-circle me-2"></i>Create account</a>
        @endif
    </form>
</main>
<footer class="footer">
    <div class="small text-muted text-end m-1">Tigase XMPP Server ${XMPPServer.getImplementationVersion()}</div>
</footer>
</body>
</html>