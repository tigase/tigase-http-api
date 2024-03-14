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
        <h4 class="card-header p-3">Security of setup</h4>
        <div class="card-body">
            <div class="row mb-3">
                <p>By default Tigase XMPP Server is configured to allow access to this setup page only user authorized by JID and password listed as admins.</p>
                <p>However in rare conditions it may be impossible to access database for authentication. In this case we use separate admin credentials stored in <i>etc/init.properties</i> file.</p>
                <p>Please enter username and password you wish to use in this situation. If you leave it blank, setup will be inaccessible without connection to database.</p>
            </div>
            <div class="row mb-3">
                <label class="col-sm-3 col-form-label" for="username">Username</label>
                <div class="col-sm-9">
                    <input class="form-control" name="username" type="text" size="30" value="${config.getSetupUser()}" />
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-3 col-form-label" for="password">Password</label>
                <div class="col-sm-9">
                    <input class="form-control" name="password" type="password" size="30" value="${config.getSetupPassword()}"/>
                </div>
            </div>
        </div>
    </div>
    <div class="m-3">
        <button class="btn btn-secondary" name="submit" type="next">Next</button>
    </div>
</form>
`)