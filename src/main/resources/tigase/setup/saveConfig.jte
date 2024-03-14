@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import tigase.http.modules.setup.pages.FeaturesPage
@import java.util.List
@param Config config
@param List<SetupHandler> pages
@param SetupHandler currentPage
@template.layout(pages = pages, currentPage = currentPage, content = @`
<form class="form-horizontal" method='POST'>
    <div class="card mt-3">
        <h4 class="card-header p-3">Installation of Tigase XMPP Server is almost finished</h4>
        <div class="card-body">
            <div class="mb-3">
                <label class="col-form-label" for='config'>Configuration created during installation:</label>
                <textarea class="form-control" name='config' rows='20' cols='80'>${ config.getAsDsl() }</textarea>
            </div>
        </div>
    </div>
    <div class="m-3">
        <button class="btn btn-secondary" name="submit" type="next">Save</button>
    </div>
</form>
`)