@import jakarta.ws.rs.core.SecurityContext
@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.UsersHandler
@import javax.servlet.http.HttpServletRequest
@import java.util.List
@param SecurityContext securityContext
@param UriInfo uriInfo
@param HttpServletRequest request
@param List<String> avatars

@template.layout(securityContext = securityContext, uriInfo = uriInfo, request = request, content = @`
    <div class="card m-3 bg-white">
        <div class="card-body">
            <div class="card-title row m-0">
                <div class="row">
                    <a href="${uriInfo.getBaseUriBuilder().path(UsersHandler.class, "index").build().toString()}" class="text-decoration-none"><h6 class="text-secondary">Users</h6></a>
                </div>
                <h5 class="col">Avatars pool</h5>
                <div class="col-auto align-items-end">
                    <button class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#upload-avatar"><i class="bi bi-plus-circle me-2"></i>Upload avatar</button>
                </div>
                @template.imageUpload(modalId = "upload-avatar", url = uriInfo.getRequestUri().toString(), title = "Upload avatar", message = "Select avatar to upload to avatar pool", deleteAvailable = false)
            </div>
            <div class="row flex-wrap align-items-start my-2">
                @for(String key : avatars)
                    <div class="col-auto">
                    <div class="position-relative">
                        !{ var url = uriInfo.getRequestUriBuilder().path(key).build().toString(); }
                        !{ var id = "avatar-" + key; }
                        <img style="width: 5em; height: 5em;" class="rounded-circle mx-2 mb-4 mt-1 d-block" src="${url}">
                        <button class="btn btn-circle btn-small position-absolute bg-danger" style="z-index: 9; margin: 1rem; top: 3rem; left: 3rem; --bs-bg-opacity: 0.8" data-bs-toggle="modal" data-bs-target="#${id}">
                            <i class="bi bi-trash text-white" style="font-size: 1rem;"></i>
                        </button>
                        <div class="modal" id="${id}">
                            <div class="modal-dialog modal-lg">
                                <div class="modal-content">
                                    <!-- Modal Header -->
                                    <div class="modal-header">
                                        <h5 class="modal-title">Confirm</h5>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                    </div>

                                    <!-- Modal body -->
                                    <div class="modal-body">
                                        <div class="mb-1">
                                            <p>Do you wish to delete avatar displayed below from the pool?</p>
                                            <img style="width: 5em; height: 5em;" class="rounded-circle mx-2 d-block" src="${url}">
                                        </div>
                                    </div>

                                    <!-- Modal footer -->
                                    <div class="modal-footer">
                                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                                        <button type="button" class="btn btn-danger" onclick="deleteAvatarFromPool('${url}')">Delete</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    </div>
                @endfor
            </div>
        </div>
        <script>
            function deleteAvatarFromPool(url) {
                let formData = new FormData();
                fetch(url, {
                    method: 'DELETE',
                    body: formData
                }).then(result => {
                    if (result.ok) {
                        document.location.reload();
                    } else {
                        alert('Avatar delete failed. Please try again.');
                    }
                }).catch(error => {
                    console.error("Uncaught error:", error.message);
                })
            }
        </script>
    </div>
`)
