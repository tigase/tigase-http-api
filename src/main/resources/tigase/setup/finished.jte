@import tigase.http.modules.setup.SetupHandler
@import java.util.List
@param List<SetupHandler> pages
@param SetupHandler currentPage
@template.layout(pages = pages, currentPage = currentPage, content = @`
<form class="form-horizontal" method='POST'>
    <div class="card mt-3">
        <h4 class="card-header p-3">Installation of Tigase XMPP Server finished</h4>
        <div class="card-body">
            <div class="mb-3">
                <div class="alert alert-success" role="alert">
                    Setup completed, please restart the server now.
                </div>
            </div>
        </div>
    </div>
</form>
`)