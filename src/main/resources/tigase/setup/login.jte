<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta charset="UTF-8"/>
    <title>Tigase XMPP Server: Setup</title>
    <link rel="stylesheet" type="text/css" href="assets/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="assets/css/custom.css">
    <link rel="stylesheet" type="text/css" href="assets/css/http-api-custom.css">
    <style>
        body {
            font-family: var(--bs-body-font-family);
        }
        .footer {
            font-size: inherit;
        }
        img.logo {
            content: url(assets/images/logo.png);
        }
    </style>
</head>
<body class="d-flex flex-column min-vh-100">
<noscript>
    <div style="border: 2px solid red; margin: 3px">
        <div style="color: red; font-weight: bold; padding: 10px "><span>WARNING:</span><span>Your browser does not support JavaScript or JavaScript is disabled.</span></div>
        <div style="font-size: 0.9em; padding: 5px 15px">Tigase XMPP Server Setup uses JavaScript to validate chosen configuration options - it may not work properly without JavaScript and may result in an incorrect configuration.<br/>We strongly recommend changing web browser or enabling JavaScript in web browser configuration.</div>
    </div>
</noscript>
<main class="text-center m-auto" style="height: unset">
    <form style="width: 300px" method="post">
        <img class="logo mb-4" style="width: 170px">
        <h1 class="h3 mb-3 fw-normal">Tigase XMPP Server</h1>
        <div class="form-floating mb-3">
            <input id="username" type="text" name="username" placeholder="ser" class="form-control" required>
            <label for="username">Username</label>
        </div>
        <div class="form-floating mb-3">
            <input id="password" type="password" name="password" placeholder="*****" class="form-control" required>
            <label for="password">Password</label>
        </div>
        <button type="submit" class="w-100 btn btn-lg btn-primary"><i class="bi bi-key me-2"></i>Sign in</button>
    </form>
</main>
<footer class="footer">
    <div class="small text-muted text-end m-1">Tigase XMPP Server ${tigase.server.XMPPServer.getImplementationVersion()}</div>
</footer>
<script src="assets/js/bootstrap.bundle.min.js"></script>
</body>
</html>