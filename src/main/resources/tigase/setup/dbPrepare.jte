@import tigase.db.util.SchemaLoader
@import tigase.db.util.SchemaManager
@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import tigase.http.modules.setup.pages.DatabaseConfigPage
@import tigase.http.modules.setup.pages.DatabasePreparationPage
@import java.util.List
@import java.util.Map
@param Config config
@param List<SetupHandler> pages
@param DatabasePreparationPage currentPage
@param List<SchemaManager.Pair<SchemaManager.DataSourceInfo, List<DatabasePreparationPage.ResultEntry>>> result
@template.layout(pages = pages, currentPage = currentPage, content = @`
<form class="form-horizontal" method='POST'>
    @for(SchemaManager.Pair<SchemaManager.DataSourceInfo, List<DatabasePreparationPage.ResultEntry>> dataSourceEntry : result)
        <div class="card mt-3">
            <h4 class="card-header p-3">You have selected database with URI = ${dataSourceEntry.getKey().getResourceUri()}</h4>
            <div class="card-body">
                @for(DatabasePreparationPage.ResultEntry entry : dataSourceEntry.getValue())
                    <div class="card m-3">
                        <h5 class="card-header p-3 w-100 ${entry.getHeaderBackgroundClass()} bg-opacity-10">
                            <span style="float: left;" class="${entry.getResultTextClass()}">${entry.getName()}</span>
                            <span style="float: right;" class="${entry.getResultTextClass()}">${entry.getResultName().toUpperCase()}</span>
                        </h5>
                        @if(entry.getMessage() != null)
                            <div class="card-body">
                                <pre style="margin-bottom: 0px;" class="font-monospace">${entry.getMessage()}</pre>
                            </div>
                        @endif
                    </div>
                @endfor
            </div>
        </div>
    @endfor
    <div class="m-3">
        <button class="btn btn-secondary" name="submit" type="next">Next</button>
    </div>
</form>
`)