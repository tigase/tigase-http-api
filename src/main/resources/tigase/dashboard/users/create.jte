@import jakarta.ws.rs.core.UriInfo
@import java.util.List
@param UriInfo uriInfo
@param List<String> domains
<div class="modal fade" id="createUserModal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="createUserModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <form action="${uriInfo.getRequestUriBuilder().path("create").build().toString()}" method="post">
            <div class="modal-header">
                <h1 class="modal-title fs-5" id="createUserModalLabel">Create user</h1>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="form-floating mb-3">
                    <input type="text" class="form-control" id="localpart" name="localpart" placeholder="user-1" required>
                    <label for="localpart">Username</label>
                </div>
                <div class="form-floating">
                    <select class="form-select mb-3" id="domain" name="domain" required>
                        @for(String domain : domains)
                            <option value="${domain}">${domain}</option>
                        @endfor
                    </select>
                    <label for="domain">Select domain</label>
                </div>
                <div class="form-floating mb-3">
                    <input type="password" class="form-control" id="password" name="password" placeholder="*****">
                    <label for="password">Password</label>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                <button type="submit" class="btn btn-primary">Create</button>
            </div>
            </form>
        </div>
    </div>
</div>
