@import gg.jte.Content
@import jakarta.ws.rs.core.SecurityContext
@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.DashboardHandler
@import tigase.http.modules.dashboard.DomainsHandler
@import tigase.http.modules.dashboard.ServerInfoHandler
@import tigase.http.modules.dashboard.UsersHandler
@import javax.servlet.http.HttpServletRequest
@param SecurityContext securityContext
@param UriInfo uriInfo
@param HttpServletRequest request
@param Content content
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/html">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=0.86">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta charset="UTF-8"/>
    <title>Tigase XMPP Server</title>
    <style>
        .sidebar-brand img.logo {
            content: url(${uriInfo.getBaseUriBuilder().path("/assets/images/logo.png").build().getPath()});
            width: 3em;
        }
    </style>
    @template.head-common(uriInfo = uriInfo)
    <style>
        body {
            font-family: var(--bs-body-font-family);
        }
        .dropdown-menu > li > a {
            color: #1e2125;
        }
        .navbar-header a.sidebar-brand {
            text-decoration: none;
            font-size: 1.1em;
        }
    </style>
    <style>
        .copyable:hover > i {
            opacity: 1.0;
        }
        .copyable i {
            opacity: 0.5;
        }
    </style>
    <script>
        function copyTextToClipboard(el, isDiv) {
            event.stopPropagation();

            let source = isDiv ? el : el.querySelector("pre");
            let text = source.textContent;
            let i = el.querySelector("i");
            i.classList.remove("bi-clipboard-fill");
            i.classList.add("bi-clipboard-check-fill");
            window.setTimeout(()=> {
                i.classList.add("bi-clipboard-fill");
                i.classList.remove("bi-clipboard-check-fill");
            }, 500);
            navigator.clipboard.writeText(text);
        }

        window.addEventListener("load", ()=> {
            let copyableItems = document.getElementsByClassName("copyable");
            for (let copyable of copyableItems) {
                let isDiv = copyable.nodeName == 'DIV';
                let copy = document.createElement("i");
                copy.classList.add("mx-2","bi","bi-clipboard-fill");
                if (isDiv) {
                    copy.setAttribute("style", "font-size: 1.1em; margin-top: -0.1em; margin-bottom: -0.1em; display: block; position: absolute; top: 1em; right: 1em;")
                } else {
                    copy.setAttribute("style", "font-size: 1.1em; margin-top: -0.1em; margin-bottom: -0.1em; display: inline-block;")
                }
                copyable.append(copy);
                copyable.addEventListener("click", ()=> {
                    copyTextToClipboard(copyable, isDiv)
                });
            }
        });
        // function copyTextToClipboard(el) {
        //     event.stopPropagation();
        //     var text = $(el).text();
        //     let iEls = $(el).find("i");
        //     if (iEls != null) {
        //         for (let iEl of iEls) {
        //             $(iEl).removeClass("bi-clipboard-fill");
        //             $(iEl).addClass("bi-clipboard-check-fill");
        //         }
        //         window.setTimeout(function() {
        //             for (let iEl of iEls) {
        //                 $(iEl).addClass("bi-clipboard-fill");
        //                 $(iEl).removeClass("bi-clipboard-check-fill");
        //             }
        //         }, 500);
        //     }
        //     navigator.clipboard.writeText(text);
        // }
        //
        // $(window).on("load", function() {
        //     for (let copyable of $(".copyable")) {
        //         $(copyable).append("<i class=\"mx-2 bi bi-clipboard-fill\" style=\"font-size: 1.1em; margin-top: -0.1em; margin-bottom: -0.1em; display: inline-block;\"></i>");
        //         $(copyable).on("click", function() {
        //             copyTextToClipboard(this);
        //         });
        //     }
        // });
    </script>
</head>
<body class="vh-100">
<noscript>
    <div style="border: 2px solid red; margin: 3px">
        <div style="color: red; font-weight: bold; padding: 10px "><span>WARNING:</span><span>Your browser does not support JavaScript or JavaScript is disabled.</span></div>
        <div style="font-size: 0.9em; padding: 5px 15px">Tigase XMPP Server uses JavaScript to validate chosen configuration options - it may not work properly without JavaScript and may result in an incorrect configuration.<br/>We strongly recommend changing web browser or enabling JavaScript in web browser configuration.</div>
    </div>
</noscript>
<main role="main" class="d-flex flex-nowrap vh-100">
    <div class="d-flex flex-column flex-shrink-0 text-white bg-dark" style="width: 250px">
        <div class="navbar-header text-center px-2 py-3">
            <a class="sidebar-brand d-flex align-items-center" href="${uriInfo.getBaseUri().toString()}" id="logo">
                <img class="logo"/><span class="align-middle ms-2 text-white">Tigase XMPP Server</span>
            </a>
        </div>
        <ul class="nav nav-pills flex-column px-3">
            @if(DashboardHandler.canAccess(securityContext, UsersHandler.class, "index"))
            <li class="nav-item">
                !{var link = uriInfo.getBaseUriBuilder().path(UsersHandler.class,"index").build().getPath();}
                <a class="nav-link text-white ${request.getRequestURI().equals(link) ? "active" : ""}" aria-current="page" href="${link}"><i class="bi bi-people-fill me-2"></i>Users</a>
            </li>
            @endif
            @if(DashboardHandler.canAccess(securityContext, DomainsHandler.class, "index"))
                <li class="nav-item">
                    !{var link = uriInfo.getBaseUriBuilder().path(DomainsHandler.class,"index").build().getPath();}
                    <a class="nav-link text-white ${request.getRequestURI().equals(link) ? "active" : ""}" aria-current="page" href="${link}"><i class="bi bi-server me-2"></i>Domains</a>
                </li>
            @endif
            @if (DashboardHandler.canAccess(securityContext, ServerInfoHandler.class, "info"))
                <li class="nav-item">
                    !{var link = uriInfo.getBaseUriBuilder().path(ServerInfoHandler.class,"info").build().getPath();}
                    <a class="nav-link text-white ${request.getRequestURI().equals(link) ? "active" : ""}" aria-current="page" href="${link}"><i class="bi bi-clipboard-data me-2"></i>Server info</a>
                </li>
            @endif
            <%--            <li class="nav-item">--%>
<%--                <a class="nav-link text-white" href="#"><i class="bi bi-gear-fill me-2"></i>Other</a>--%>
<%--            </li>--%>
        </ul>
        <hr class="mx-2 mt-auto mb-0"/>
        @if (securityContext.getUserPrincipal() != null)
        <div class="dropdown m-2">
            <a id="dropdownUser" class="d-flex align-items-center text-white text-decoration-none dropdown-toggle px-2" data-bs-toggle="dropdown" aria-expanded="false">
                <i class="bi bi-person-circle me-2" style="font-size: 1.2em"></i>
                <span class="align-middle fw-bolder w-100 text-truncate">${securityContext.getUserPrincipal().getName()}</span>
            </a>
            <ul class="dropdown-menu dropdown-menu-dark text-small shadow w-100" aria-labelledby="dropdownUser">
                <li>
                    <form method="post" action="${uriInfo.getBaseUriBuilder().path("/logout").build().getPath()}">
                        <button class="dropdown-item" type="submit"><i class="bi bi-box-arrow-in-right me-2"></i>Sign out</button>
                    </form>
                </li>
            </ul>
        </div>
        @endif
    </div>
    <div class="d-flex flex-column bg-light w-100 overflow-auto">
        ${content}
        <footer class="footer mt-auto">
            <div class="container-fluid d-flex flex-row-reverse">
                <p class="text-secondary p-0 m-1">Powered by Tigase XMPP Server ${tigase.server.XMPPServer.getImplementationVersion()}</p>
            </div>
        </footer>
    </div>
</main>
<script src="${uriInfo.getBaseUriBuilder().path("assets/js/bootstrap.bundle.min.js").build().getPath()}"></script>
</body>
</html>