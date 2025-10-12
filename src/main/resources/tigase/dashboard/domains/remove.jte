@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.DomainsHandler
@param UriInfo uriInfo
@param String domain
<div class="modal fade" id="deleteDomain-${domain.replace('@','-').replace('.','-')}" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="#deleteDomainModalLabel-${domain.replace('@','-').replace('.','-')}" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <form action="${uriInfo.getBaseUriBuilder().path(DomainsHandler.class, "delete").build(domain).toString()}" method="post">
                <div class="modal-header">
                    <h1 class="modal-title fs-5" id="deleteDomainModalLabel-${domain.replace('@','-').replace('.','-')}">Delete domain</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p>Are you sure that you want to remove domain <span class="fw-bold">${domain}</span>?</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Delete</button>
                </div>
            </form>
        </div>
    </div>
</div>