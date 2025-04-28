@import jakarta.ws.rs.core.SecurityContext
@import jakarta.ws.rs.core.UriBuilder
@import jakarta.ws.rs.core.UriInfo
@import tigase.db.AuthRepository
@import tigase.http.jaxrs.Page
@import tigase.http.modules.dashboard.DashboardHandler
@import tigase.http.modules.dashboard.DashboardModule
@import tigase.http.modules.dashboard.UsersHandler
@import tigase.xmpp.jid.BareJID
@import javax.servlet.http.HttpServletRequest
@import java.util.List
@param SecurityContext securityContext
@param UriInfo uriInfo
@param Page<UsersHandler.User> users
@param List<String> domains
@param Boolean accountExpirationServiceEnabled
@param String query
@param HttpServletRequest request
@param boolean isXTokenActive
@param List<UsersHandler.UserRole> allRoles

@template.layout(securityContext = securityContext, uriInfo = uriInfo, request = request, content = @`
    <div class="card m-3 bg-white">
        <div class="card-body">
            <div class="card-title row m-0">
                <h5 class="col">Users</h5>
                <div class="col-auto align-items-end">
                    <button class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#createUserModal"><i class="bi bi-person-fill-add me-2"></i>Add user</button>
                    @template.users.create(uriInfo = uriInfo, domains = domains, accountExpirationServiceEnabled = accountExpirationServiceEnabled)
                </div>
            </div>
            <div class="container-fluid">
                <form class="row row-cols-sm-auto g-3 align-items-center">
                    <div class="col-12">
                        <label class="visually-hidden" for="query">Search</label>
                        <div class="input-group input-group-sm">
                            <div class="input-group-text">
                                <i class="bi bi-search"></i>
                            </div>
                            <input type="text" class="form-control form-control-sm" id="query" name="query" value="${query}">
                            <button type="submit" class="btn btn-sm btn-primary">Search</button>
                        </div>
                    </div>
                </form>
            </div>
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th scope="col">JID</th>
                        <th scope="col">Account status</th>
                        <th scope="col">Roles</th>
                        <th scope="col" style="width: 0px;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                @for(UsersHandler.User user : users.items())
                    <tr>
                        <td>${user.jid().toString()}</td>
                        <td>
                            @if(user.accountStatus() == AuthRepository.AccountStatus.active)
                                <i class="bi bi-check-circle me-2 text-primary"></i>Active
                            @else
                                <i class="bi bi-ban me-2"></i>Disabled
                            @endif
                        </td>
                        <td>
                            @for(UsersHandler.UserRole role : user.roles())
                                <div class="badge small bg-secondary rounded-pill">${role.label()}</div>
                            @endfor
                        </td>
                        <td style="width: 0px">
                            <div class="dropdown">
                                <button class="btn btn-secondary btn-sm dropdown-toggle ${(!user.canManageUser()) ? "disabled" : ""}" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                    <i class="bi bi-gear-fill me-2"></i>Actions
                                </button>
                                <ul class="dropdown-menu">
                                    @if(user.canManageUser())
                                    <li>
                                        @if(user.accountStatus() == AuthRepository.AccountStatus.active)
                                            <a class="dropdown-item text-nowrap" href="${uriInfo.getBaseUriBuilder().path(UsersHandler.class, "changeAccountStatus").build(user.jid(), AuthRepository.AccountStatus.disabled).toString()}"><i class="bi bi-person-fill-lock me-2"></i>Disable</a>
                                        @else
                                            <a class="dropdown-item text-nowrap" href="${uriInfo.getBaseUriBuilder().path(UsersHandler.class, "changeAccountStatus").build(user.jid(), AuthRepository.AccountStatus.active).toString()}"><i class="bi bi-person-fill-check me-2"></i>Enable</a>
                                        @endif
                                    </li>
                                    @if(DashboardHandler.canAccess(securityContext, UsersHandler.class, "updateRoles"))
                                    <li>
                                        <a class="dropdown-item text-nowrap" href="#" data-bs-toggle="modal"
                                           data-bs-target="#manageRoles-${user.jid().toString().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-card-list me-2"></i>Manage roles
                                        </a>
                                    </li>
                                    @endif
                                    <li>
                                        <a class="dropdown-item text-warning text-nowrap" href="#" data-bs-toggle="modal"
                                           data-bs-target="#changePassword-${user.jid().toString().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-safe2-fill me-2"></i>Change password
                                        </a>
                                    </li>
                                    @if(accountExpirationServiceEnabled)
                                    <li>
                                        <a class="dropdown-item text-warning text-nowrap" href="#" data-bs-toggle="modal"
                                           data-bs-target="#setAccountExpiration-${user.jid().toString().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-clock-history me-2"></i> Set account expiration
                                        </a>
                                    </li>
                                    @endif
                                    @if(isXTokenActive)
                                    <li>
                                        <a class="dropdown-item text-warning text-nowrap" href="#" data-bs-toggle="modal"
                                           data-bs-target="#generateAuthToken-${user.jid().toString().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-qr-code me-2"></i>New QR code
                                        </a>
                                    </li>
                                    @endif
                                    <li>
                                        <a class="dropdown-item text-danger text-nowrap" href="#" data-bs-toggle="modal"
                                                 data-bs-target="#deleteUser-${user.jid().toString().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-person-fill-dash me-2"></i>Delete
                                        </a>
                                    </li>
                                    @endif
                                </ul>
                            </div>
<%--                            <button class="btn btn-sm btn-danger text-nowrap" data-bs-toggle="modal"--%>
<%--                                    data-bs-target="#deleteUser-${user.jid().toString().replace('@','-').replace('.','-')}">--%>
<%--                                <i class="bi bi-person-fill-dash me-2"></i>Delete--%>
<%--                            </button>--%>
                            @if(user.canManageUser())
                            @if(DashboardHandler.canAccess(securityContext, UsersHandler.class, "updateRoles"))
                            <div class="modal fade" id="manageRoles-${user.jid().toString().replace('@','-').replace('.','-')}" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="#manageRolesModalLabel-${user.toString().replace('@','-').replace('.','-')}" aria-hidden="true">
                                <div class="modal-dialog">
                                    <div class="modal-content">
                                        <form action="${uriInfo.getRequestUriBuilder().path(user.jid().toString()).path("roles").build().toString()}" method="post">
                                            <div class="modal-header">
                                                <h1 class="modal-title fs-5" id="manageRolesModalLabel-${user.jid().toString().replace('@','-').replace('.','-')}">Manage roles</h1>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <p>Select roles that user should have:</p>
                                                @for(UsersHandler.UserRole role : allRoles)
                                                <div class="form-check form-switch">
                                                    <input class="form-check-input" type="checkbox" role="switch" name="roles" id="role-switch-${role.id()}" checked="${user.hasRole(role)}" value="${role.id()}">
                                                    <label class="form-check-label" for="role-switch-${role.id()}">${role.label()}</label>
                                                </div>
                                                @endfor
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                                <button type="submit" class="btn btn-primary">Change</button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                            @endif
                            <div class="modal fade" id="deleteUser-${user.jid().toString().replace('@','-').replace('.','-')}" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="#deleteUserModalLabel-${user.toString().replace('@','-').replace('.','-')}" aria-hidden="true">
                                <div class="modal-dialog">
                                    <div class="modal-content">
                                        <form action="${uriInfo.getRequestUriBuilder().path(user.jid().toString()).path("delete").build().toString()}" method="post">
                                            <div class="modal-header">
                                                <h1 class="modal-title fs-5" id="deleteUserModalLabel-${user.jid().toString().replace('@','-').replace('.','-')}">Delete user</h1>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <p>Are you sure that you want to delete account for user ${user.jid().toString()}?</p>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                                <button type="submit" class="btn btn-primary">Delete</button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                            <div class="modal fade" id="changePassword-${user.jid().toString().replace('@','-').replace('.','-')}" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="#changePasswordModalLabel-${user.toString().replace('@','-').replace('.','-')}" aria-hidden="true">
                                <div class="modal-dialog">
                                    <div class="modal-content">
                                        <form action="${uriInfo.getRequestUriBuilder().path(user.jid().toString()).path("password").build().toString()}" method="post">
                                            <div class="modal-header">
                                                <h1 class="modal-title fs-5" id="changePasswordModalLabel-${user.jid().toString().replace('@','-').replace('.','-')}">Change password</h1>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <div class="form-floating mb-3">
                                                    <input type="password" class="form-control" id="password" name="password" required>
                                                    <label for="password">New password</label>
                                                </div>
                                                <div class="form-floating mb-3">
                                                    <input type="password" class="form-control" id="password-confirm" name="password-confirm" required>
                                                    <label for="password-confirm">Confirm password</label>
                                                </div>                                             
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                                <button type="submit" class="btn btn-primary">Change password</button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                            @if(accountExpirationServiceEnabled)
                            <div class="modal fade" id="setAccountExpiration-${user.jid().toString().replace('@','-').replace('.','-')}" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="#setAccountExpirationModalLabel-${user.toString().replace('@','-').replace('.','-')}" aria-hidden="true">
                                <div class="modal-dialog">
                                    <div class="modal-content">
                                        <form action="${uriInfo.getRequestUriBuilder().path(user.jid().toString()).path("expiration").build().toString()}" method="post">
                                            <div class="modal-header">
                                                <h1 class="modal-title fs-5" id="setAccountExpirationModalLabel-${user.jid().toString().replace('@','-').replace('.','-')}">Set account expiration</h1>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <div class="form-floating mb-3">
                                                    <input type="number" class="form-control" id="expiration" name="expiration">
                                                    <label for="expiration">Account expiration (in days; 0 disables expiration)</label>
                                                </div>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                                <button type="submit" class="btn btn-primary">Set account expiration</button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                            @endif
                            @if(isXTokenActive)
                            <div class="modal fade" id="generateAuthToken-${user.jid().toString().replace('@','-').replace('.','-')}" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="#generateAuthTokenModalLabel-${user.toString().replace('@','-').replace('.','-')}" aria-hidden="true">
                                <div class="modal-dialog">
                                    <div class="modal-content">
                                        <form action="${uriInfo.getBaseUriBuilder().path(UsersHandler.class,"generateAuthQrCodeJson").build(user.jid().toString()).toString()}" method="post">
                                            <div class="modal-header">
                                                <h1 class="modal-title fs-5" id="generateAuthTokenModalLabel-${user.jid().toString().replace('@','-').replace('.','-')}">Generate new token</h1>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <p>Do you wish to generate a new authentication token for user ${user.jid().toString()}?</p>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                                <a type="submit" class="btn btn-primary" onclick="generateQrCode('${user.jid().toString()}')">Generate</a>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                            @endif
                            @endif
                        </td>
                    </tr>
                @endfor
                </tbody>
                @if(isXTokenActive)
                <script>
                    let generateQrCode = function (jid) {
                        let escapedJid = jid.replaceAll("@","-").replaceAll(".","-");
                        let modal = document.querySelector("#generateAuthToken-" + escapedJid);
                        let modalBody = document.querySelector("#generateAuthToken-" + escapedJid + " div.modal-body");
                        let modalContentOriginal = modalBody.innerHTML;
                        let generateButton = document.querySelector("#generateAuthToken-" + escapedJid + " div.modal-footer a[type='submit']");
                        let originalGenerateButtonLabel = generateButton.textContent;
                        let closeButton = document.querySelector("#generateAuthToken-" + escapedJid + " div.modal-footer button[type='button']");
                        let modalFooter = generateButton.parentElement;
                        let spinner = document.createElement("div");
                        let closed = false;
                        modal.addEventListener("hide.bs.modal", function () {
                            closed = true;
                            spinner.remove();
                            modalBody.innerHTML = modalContentOriginal;
                            modalFooter.innerHTML = "";
                            modalFooter.appendChild(closeButton);
                            modalFooter.appendChild(generateButton);
                            generateButton.classList.remove("disabled");
                            generateButton.innerHTML = "";
                            generateButton.append(document.createTextNode(originalGenerateButtonLabel));
                        });
                        // remove existing error message
                        document.querySelector("#generateAuthToken-" + escapedJid + " div.modal-body span.error-message")?.remove();
                        // update "generate" button label
                        generateButton.classList.add("disabled");
                        generateButton.innerHTML = "";
                        spinner.setAttribute("role", "status");
                        spinner.classList.add("spinner-border", "spinner-border-sm", "me-2");
                        generateButton.append(spinner);
                        generateButton.append(document.createTextNode("Processing..."));
                        // send request
                        let xhr = new XMLHttpRequest();
                        let url = "${uriInfo.getBaseUriBuilder().path(UsersHandler.class,"generateAuthQrCodeJson").build("@").toString()}".replace("@", jid);
                        xhr.open("POST", url, true);
                        xhr.setRequestHeader('Content-type', "application/x-www-form-urlencoded");
                        xhr.setRequestHeader('Accept', 'application/json');
                        xhr.send("");
                        xhr.responseType = "json";
                        let handleResult = function () {
                            // update generate button
                            if (closed) {
                                // modal closed, discard result...
                                return;
                            }
                            spinner.remove();
                            generateButton.classList.remove("disabled");
                            generateButton.innerHTML = "";
                            generateButton.append(document.createTextNode(originalGenerateButtonLabel));
                            if (xhr.status === 200) {
                                // replace content of modal
                                modalBody.innerHTML = "";
                                let image = new Image();
                                image.classList.add("d-block", "mx-auto", "w-50");
                                image.src = xhr.response.png;
                                modalBody.append(image);
                                let p = document.createElement("p");
                                p.classList.add("text-secondary", "small", "w-100", "text-center", "m-0", "pt-1")
                                p.append(document.createTextNode("Scan QR code to authenticate account."))
                                modalBody.append(p);
                                // replace generate button with "save" button
                                generateButton.remove();
                                let copyToken = document.createElement("a");
                                copyToken.classList.add("btn", "btn-secondary");
                                copyToken.append(document.createTextNode("Copy token"));
                                copyToken.onclick = function () {
                                    navigator.clipboard.writeText(xhr.response.token);
                                }
                                modalFooter.append(copyToken);
                                let saveLink = document.createElement("a");
                                saveLink.classList.add("btn", "btn-secondary");
                                saveLink.append(document.createTextNode("Save to file"))
                                saveLink.href = xhr.response.png.replace("image/png","image/octet-stream");
                                saveLink.setAttribute("download", "qrcode-" + jid + ".png");
                                modalFooter.append(saveLink);
                            } else {
                                // show error
                                let errorSpan = document.createElement("span");
                                errorSpan.append(document.createTextNode("An error occurred. Please try again later."));
                                errorSpan.classList.add("text-danger", "error-message");
                                modalBody.append(errorSpan);
                            }
                        }
                        xhr.onerror = handleResult;
                        xhr.onload = handleResult;
                    }
                </script>
                @endif
            </table>
            @template.pagination(uriInfo = uriInfo, page = users)
        </div>
    </div>
`)