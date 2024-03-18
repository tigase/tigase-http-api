@import jakarta.ws.rs.core.SecurityContext
@import jakarta.ws.rs.core.UriBuilder
@import jakarta.ws.rs.core.UriInfo
@import tigase.db.AuthRepository
@import tigase.http.jaxrs.Page
@import tigase.http.modules.dashboard.UsersHandler
@import tigase.xmpp.jid.BareJID;
@import javax.servlet.http.HttpServletRequest
@import java.util.List;
@param SecurityContext securityContext
@param UriInfo uriInfo
@param Page<UsersHandler.User> users
@param List<String> domains
@param String query
@param HttpServletRequest request

@template.layout(securityContext = securityContext, uriInfo = uriInfo, request = request, content = @`
    <div class="card m-3 bg-white">
        <div class="card-body">
            <div class="card-title row m-0">
                <h5 class="col">Users</h5>
                <div class="col-auto align-items-end">
                    <button class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#createUserModal"><i class="bi bi-person-fill-add me-2"></i>Add user</button>
                    @template.users.create(uriInfo = uriInfo, domains = domains)
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
                        <th scope="col" style="width: 0px;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                @for(UsersHandler.User user : users.items())
                    <tr>
                        <td>${user.jid().toString()}</td>
                        <td>
                            @if(user.accountStatus() == AuthRepository.AccountStatus.active)
                                <i class="bi bi-toggle-on me-2 text-primary"></i>Active
                            @else
                                <i class="bi bi-toggle-off me-2"></i>Disable
                            @endif
                        </td>
                        <td>
                            <div class="dropdown">
                                <button class="btn btn-secondary btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                    <i class="bi bi-gear-fill me-2"></i>Actions
                                </button>
                                <ul class="dropdown-menu">
                                    <li>
                                        @if(user.accountStatus() == AuthRepository.AccountStatus.active)
                                            <a class="dropdown-item text-nowrap" href="${uriInfo.getBaseUriBuilder().path(UsersHandler.class, "changeAccountStatus").build(user.jid(), AuthRepository.AccountStatus.disabled).toString()}"><i class="bi bi-person-fill-lock me-2"></i>Disable</a>
                                        @else
                                            <a class="dropdown-item text-nowrap" href="${uriInfo.getBaseUriBuilder().path(UsersHandler.class, "changeAccountStatus").build(user.jid(), AuthRepository.AccountStatus.active).toString()}"><i class="bi bi-person-fill-check me-2"></i>Enable</a>
                                        @endif
                                    </li>
                                    <li>
                                        <a class="dropdown-item text-warning text-nowrap" href="#" data-bs-toggle="modal"
                                           data-bs-target="#changePassword-${user.jid().toString().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-safe2-fill me-2"></i>Change password
                                        </a>
                                    </li>
                                    <li>
                                        <a class="dropdown-item text-danger text-nowrap" href="#" data-bs-toggle="modal"
                                                 data-bs-target="#deleteUser-${user.jid().toString().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-person-fill-dash me-2"></i>Delete
                                        </a>
                                    </li>
                                </ul>
                            </div>
<%--                            <button class="btn btn-sm btn-danger text-nowrap" data-bs-toggle="modal"--%>
<%--                                    data-bs-target="#deleteUser-${user.jid().toString().replace('@','-').replace('.','-')}">--%>
<%--                                <i class="bi bi-person-fill-dash me-2"></i>Delete--%>
<%--                            </button>--%>
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
                        </td>
                    </tr>
                @endfor
                </tbody>
            </table>
            @template.pagination(uriInfo = uriInfo, page = users)
        </div>
    </div>
`)