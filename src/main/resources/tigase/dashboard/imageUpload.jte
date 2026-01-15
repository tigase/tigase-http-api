@param String modalId
@param String url
@param String title
@param String message = ""
@param boolean deleteAvailable = true
@param int maxSize = 1024
<div class="modal" id="${modalId}">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <!-- Modal Header -->
            <div class="modal-header">
                <h5 class="modal-title">${title}</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>

            <!-- Modal body -->
            <div class="modal-body">
                <div class="mb-1">
                    @if(message != null)
                    <p>${message}</p>
                    @endif
                    <input class="form-control form-control-sm" type="file" name="file" id="${modalId}-file" accept=".jpg,.jpeg,image/jpeg">
                    <div id="${modalId}-imageContainer" class="img-container d-none mt-2">
                        <img id="${modalId}-image" style="display: block; max-width: 100%;">
                    </div>
                </div>
            </div>

            <!-- Modal footer -->
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                @if(deleteAvailable)
                <button id="${modalId}-delete" type="button" class="btn btn-danger">Delete</button>
                @endif
                <button id="${modalId}-submit" type="button" class="btn btn-primary" disabled>Submit</button>
            </div>
            <script>
                {
                    const logoUploadURL = "$unsafe{url}";
                    const modalId = "$unsafe{modalId}";
                    const url = "$unsafe{url}";
                    const maxSize = ${maxSize};

                    document.getElementById(modalId).addEventListener('show.bs.modal', (event) => {
                        let image = document.getElementById(modalId + '-image');
                        image.src = url;
                    })

                    window.addEventListener('DOMContentLoaded', function () {
                        let image = document.getElementById(modalId + '-image');
                        let cropper;

                        let deleteId = modalId + '-delete';
                        document.getElementById(deleteId)?.addEventListener('click', ()=>{
                            let formData = new FormData();
                            fetch(logoUploadURL, {
                                method: 'DELETE',
                                body: formData
                            }).then(result => {
                                if (result.ok) {
                                    document.location.reload();
                                } else {
                                    alert('File delete failed. Please try again.');
                                }
                            }).catch(error => {
                                console.error("Uncaught error:", error.message);
                            })
                        })

                        let submitId = modalId + '-submit';
                        document.getElementById(submitId).addEventListener('click', function () {
                            if (cropper != null) {
                                cropper.getCroppedCanvas({
                                    minWidth: 256,
                                    minHeight: 256,
                                    maxWidth: maxSize,
                                    maxHeight: maxSize,
                                    imageSmoothingEnabled: false,
                                    imageSmoothingQuality: 'high',
                                }).toBlob((blob) => {
                                    const formData = new FormData();
                                    formData.append('file', blob);
                                    fetch(logoUploadURL, {
                                        method: 'POST',
                                        body: formData
                                    }).then(result => {
                                        console.log("upload result:", result)
                                        if (result.ok) {
                                            document.location.reload();
                                        } else {
                                            alert('File upload failed. Please try again.');
                                        }
                                    }).catch(error => {
                                        console.error("Uncaught error:", error.message);
                                    })
                                }, 'image/jpeg');
                            }
                        });

                        document.getElementById(modalId + '-file').addEventListener("change", function (e) {
                            var imgsrc = URL.createObjectURL(e.target.files[0]);
                            document.getElementById(submitId).disabled = !imgsrc;
                            if (imgsrc) {
                                document.getElementById(modalId + '-image').setAttribute("src", imgsrc);
                                document.getElementById(modalId + '-imageContainer').classList.remove("d-none");
                                if (cropper) {
                                    cropper.destroy();
                                }
                                cropper = new Cropper(image, {
                                    viewMode: 1,
                                    aspectRatio: 1 / 1,
                                    autoCropArea: 1.0,
                                    initialAspectRatio: 1.0
                                })
                            }
                        })
                    })
                }
            </script>
        </div>
    </div>
</div>

