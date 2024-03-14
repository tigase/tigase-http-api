@import gg.jte.Content
@import tigase.http.modules.setup.SetupHandler
@import java.util.List
@param List<SetupHandler> pages
@param SetupHandler currentPage
@param Content content
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta charset="UTF-8"/>
    <title>Tigase XMPP Server: Setup: ${currentPage.getTitle()}</title>
    <link rel="stylesheet" type="text/css" href="assets/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="assets/css/custom.css">
    <link rel="stylesheet" type="text/css" href="assets/css/http-api-custom.css">
</head>
<body class="d-flex flex-column min-vh-100">
<noscript>
    <div style="border: 2px solid red; margin: 3px">
        <div style="color: red; font-weight: bold; padding: 10px "><span>WARNING:</span><span>Your browser does not support JavaScript or JavaScript is disabled.</span></div>
        <div style="font-size: 0.9em; padding: 5px 15px">Tigase XMPP Server Setup uses JavaScript to validate chosen configuration options - it may not work properly without JavaScript and may result in an incorrect configuration.<br/>We strongly recommend changing web browser or enabling JavaScript in web browser configuration.</div>
    </div>
</noscript>
<nav class="navbar navbar-expand-md navbar-dark" role = "navigation" style="margin-bottom: 0;">
    <div class="navbar-header">
        <a class="navbar-brand" href="/" id="logo">
            <p>${currentPage.getTitle()}</p>
        </a>
    </div>
</nav>
<main class="container-fluid p-0 d-flex flex-grow-1" role="main">
    @if(pages.size() != 1)
        <div class="sidebar d-flex" >
            <div class="ps-0 w-100">
                <ul class="list-group list-group-flush">
                    @for(SetupHandler page : pages)
                        <a class="list-group-item list-group-item-action@if(page==currentPage) active@endif" href="${page.getPath().substring(1)}"><i class="fa fa-fw"></i>${page.getTitle()}</a>
                    @endfor
                </ul>
            </div>
        </div>
    @endif
    <div class="flex-grow-1 d-flex flex-column">
        <div class="ps-4 pe-4">
            ${content}
        </div>
        <footer class="footer mt-auto">
            <div class="container-fluid d-flex flex-row-reverse">
                <p class="text-secondary">Powered by Tigase XMPP Server ${tigase.server.XMPPServer.getImplementationVersion()}</p>
            </div>
        </footer>
    </div>
</main>
<script src="assets/js/bootstrap.bundle.min.js"></script>
</body>
</html>