@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.DomainsHandler
@param UriInfo uriInfo
@param DomainsHandler.DomainItem domain
<div class="modal fade" id="changeStatusDomain-${domain.domain().replace('@','-').replace('.','-')}" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="#changeStatusDomainModalLabel-${domain.domain().replace('@','-').replace('.','-')}" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <form action="${uriInfo.getBaseUriBuilder().path(DomainsHandler.class, "changeStatus").build(domain.domain()).toString()}" method="post">
                <div class="modal-header">
                    <h1 class="modal-title fs-5" id="changeStatusDomainModalLabel-${domain.domain().replace('@','-').replace('.','-')}">Change domain status</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p>Are you sure that you want to change domain <span class="fw-bold">${domain.domain()}</span> status to <span class="fw-bold">${domain.isEnabled() ? "disabled" : "enabled"}</span>?</p>
                </div>
                <input type="hidden" name="value" value="${domain.isEnabled() ? "off" : "on"}">
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Change</button>
                </div>
            </form>
        </div>
    </div>
</div>