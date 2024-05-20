@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import tigase.http.modules.setup.pages.DatabaseConfigPage
@import java.util.List
@param Config config
@param List<SetupHandler> pages
@param DatabaseConfigPage currentPage
@template.layout(pages = pages, currentPage = currentPage, content = @`
<form class="form-horizontal" method='POST'>
    <div class="card mt-3">
        <h4 class="card-header p-3">You have selected ${config.getDbType().getLabel()} database. This database needs additional configuration parameters. Please enter all required information.</h4>
        <div class="card-body">
            <div class="row mb-3">
                <label class="col-sm-5 col-form-label" for="dbName">Name of the database that will be created and to which schema will be loaded</label>
                <div class="col-sm-7">
                    <input class="form-control" name="dbName" type="text" size="30" value="${config.getDbConfig().getDbName()}" required/>
                    <div class="invalid-feedback">Database name is required</div>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-5 col-form-label" for="dbHost">Address of the database instance</label>
                <div class="col-sm-7">
                    <input class="form-control" name="dbHost" type="text" size="30" value="${config.getDbConfig().getDbHost()}" required/>
                    <div class="invalid-feedback">Database <code>host</code> is required</div>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-5 col-form-label" for="dbUserName">Name of the user that will be created specifically to access Tigase XMPP Server</label>
                <div class="col-sm-7">
                    <input class="form-control" name="dbUserName" type="text" size="30" value="${config.getDbConfig().getDbUserName()}" required/>
                    <div class="invalid-feedback">Database tigase user <code>username</code> is required</div>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-5 col-form-label" for="dbUserPassword">Password of the user that will be created specifically to access Tigase XMPP Server</label>
                <div class="col-sm-7">
                    <input class="form-control" name="dbUserPassword" type="password" size="30" value="${config.getDbConfig().getDbUserPassword()}" required/>
                    <div class="invalid-feedback">Database tigase user <code>password</code> is required</div>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-5 col-form-label" for="dbRootName">Database root account username used to create tigase user and database</label>
                <div class="col-sm-7">
                    <input class="form-control" name="dbRootName" type="text" size="30" value="${config.getDbConfig().getDbRootName()}" required/>
                    <div class="invalid-feedback">Database root <code>username</code> is required</div>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-5 col-form-label" for="dbRootPassword">Database root account password used to create tigase user and database</label>
                <div class="col-sm-7">
                    <input class="form-control" name="dbRootPassword" type="password" size="30" value="${config.getDbConfig().getDbRootPassword()}" required/>
                    <div class="invalid-feedback">Database root <code>password</code> is required</div>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-5 col-form-label" for="dbUseSSL">Enable SSL support for database connection (if supported)</label>
                <div class="col-sm-7">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" name="dbUseSSL" checked="${config.getDbConfig().isDbUseSSL()}">
                    </div>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-5 col-form-label" for="dbAdditionalOptions">Additional databse options query</label>
                <div class="col-sm-7">
                    <input class="form-control" name="dbAdditionalOptions" type="text" size="30" value="${config.getDbConfig().getDbAdditionalOptions()}"/>
                </div>
            </div>
            <div class="card">
                <div class="card-body">
                    <div class="alert alert-warning mb-0" role="alert">
                        Preparing the database can take some time. Please don't refresh the page nor close the tab until it finishes.
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="m-3">
        <button class="btn btn-secondary" name="submit" type="next">Next</button>
    </div>
</form>
`)