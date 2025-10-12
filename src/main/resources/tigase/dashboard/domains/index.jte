@import jakarta.ws.rs.core.SecurityContext
@import jakarta.ws.rs.core.UriInfo
@import tigase.http.jaxrs.Page
@import tigase.http.modules.dashboard.DomainsHandler
@import tigase.http.modules.dashboard.PermissionsHelper
@import tigase.http.modules.dashboard.UsersHandler
@import javax.servlet.http.HttpServletRequest
@param SecurityContext securityContext
@param HttpServletRequest request
@param UriInfo uriInfo
@param PermissionsHelper permissionsHelper
@param Page<DomainsHandler.DomainItem> domains
@param String query

@template.layout(securityContext = securityContext, uriInfo = uriInfo, request = request, content = @`
    <div class="card m-3 bg-white">
        <div class="card-body">
            <div class="card-title row m-0">
                <h5 class="col">Domains</h5>
                @if (permissionsHelper.canCreateDomain())
                <div class="col-auto align-items-end">
                    <button class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#createDomainModal"><i class="bi bi-person-fill-add me-2"></i>Add domain</button>
                    @template.domains.add(uriInfo = uriInfo)
                </div>
                @endif
            </div>
            <div class="container-fluid">
                <form class="row row-cols-sm-auto g-3 align-items-center">
                    <div class="col-12">
                        <label class="visually-hidden" for="query">Search</label>
                        <div class="input-group input-group-sm">
                            <div class="input-group-text">
                                <i class="bi bi-search"></i>
                            </div>
                            <input type="search" class="form-control form-control-sm" id="query" name="query" value="${query}">
                            <button type="submit" class="btn btn-sm btn-primary">Search</button>
                        </div>
                    </div>
                </form>
            </div>
            <table class="table table-hover">
                <thead>
                <tr>
                    <th scope="col">Domain</th>
                    <th scope="col">Status</th>
                    <th scope="col">Users</th>
                    <th scope="col">Registration</th>
                    <th scope="col" style="width: 1px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                @for(DomainsHandler.DomainItem domain : domains.items())
                    <tr>
                        <td>${domain.domain()}</td>
                        <td>
                            @if(domain.isEnabled())
                                <i class="bi bi-check-circle me-2 text-primary"></i>Enabled
                            @else
                                <i class="bi bi-ban me-2"></i>Disabled
                            @endif
                        </td>
                        <td>${domain.noOfUsers()}@if(domain.item().getMaxUsersNumber() != 0)
                         / ${domain.item().getMaxUsersNumber()}
                        @endif
                        </td>
                        <td>
                            @if(domain.item().isRegisterEnabled())
                                <i class="bi bi-check-circle me-2 text-primary"></i>
                            @else
                                <i class="bi bi-ban me-2"></i>
                            @endif
                        </td>
                        <td style="width: 1px">
                            <div class="dropdown">
                                <button class="btn btn-secondary btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                    <i class="bi bi-gear-fill me-2"></i>Actions
                                </button>
                                <ul class="dropdown-menu">
                                    <li>
                                        <a class="dropdown-item text-nowrap" href="${uriInfo.getBaseUriBuilder().path(UsersHandler.class, "index").queryParam("query", "@"+domain.domain()).build().toString()}">
                                            <i class="bi bi-people me-2"></i>Show users
                                        </a>
                                    </li>
                                    <li>
                                        <a class="dropdown-item text-warning text-nowrap" href="#" data-bs-toggle="modal"
                                           data-bs-target="#changeStatusDomain-${domain.domain().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-lock me-2"></i>Change status
                                        </a>
                                    </li>
                                    <li>
                                        <a class="dropdown-item text-danger text-nowrap" href="#" data-bs-toggle="modal"
                                           data-bs-target="#deleteDomain-${domain.domain().replace('@','-').replace('.','-')}">
                                            <i class="bi bi-dash-circle me-2"></i>Delete
                                        </a>
                                    </li>
                                </ul>
                            </div>
                            @template.domains.changeStatus(uriInfo = uriInfo, domain = domain)
                            @template.domains.remove(uriInfo = uriInfo, domain = domain.domain())
                        </td>
                    </tr>
                @endfor
                </tbody>
            </table>
            @template.pagination(uriInfo = uriInfo, page = domains)
        </div>
    </div>
`)