@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.UserRegisterHandler
@import tigase.server.XMPPServer
@import tigase.xmpp.jid.BareJID
@import java.util.List
@param UriInfo uriInfo
@param BareJID jid
@param boolean confirmationRequired
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
    <div style="width: 450px">
        <img class="logo mb-4" style="width: 170px">
        <h1 class="h3 mb-3 fw-normal">Tigase XMPP Server</h1>

        <h5 class="h5 mb-3 fw-lighter text-secondary">Account ${jid.toString()} was successfully created!</h5>
        @if (confirmationRequired)
        <p class="text-secondary">Account <span class="font-monospace fw-bolder">${jid.toString()}</span> was created but requires confirmation. Emails with confirmation like was sent to the email you provided in the registration form.</p>
        @else
        <p class="text-secondary">You can now use your newly create XMPP account with your XMPP client to connect to Tigase XMPP Server.</p>
        @endif
        <a class="my-2 w-100 btn btn-sm btn-secondary" href="${uriInfo.getBaseUriBuilder().build().getPath()}" >Go to dashboard</a>
    </div>
</main>
<footer class="footer">
    <div class="small text-muted text-end m-1">Tigase XMPP Server ${XMPPServer.getImplementationVersion()}</div>
</footer>
</body>
</html>