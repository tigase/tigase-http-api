@import jakarta.ws.rs.core.UriInfo
@param UriInfo uriInfo
<div class="modal fade" id="createDomainModal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="createDomainModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <form action="${uriInfo.getRequestUriBuilder().path("create").build().toString()}" method="post">
            <div class="modal-header">
                <h1 class="modal-title fs-5" id="createDomainModalLabel">Add domain</h1>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="form-floating mb-3">
                    <input type="text" class="form-control" id="domain" name="domain" pattern="([A-Za-z0-9][A-Za-z0-9\-]{0,61}[A-Za-z0-9]\.)+[A-Za-z]{2,6}" title="Domain needs to be a valid DNS name" placeholder="example.com" required>
                    <label for="domain">Domain</label>
                </div>
                <div class="mb-3 form-check form-switch">
                    <input class="form-check-input" type="checkbox" role="switch" id="registrationEnabled" name="registrationEnabled">
                    <label class="form-check-label" for="registrationEnabled">Allow registration</label>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                <button type="submit" class="btn btn-primary">Add</button>
            </div>
            </form>
        </div>
    </div>
</div>
