@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import tigase.http.modules.setup.pages.FeaturesPage
@import java.util.List
@param Config config
@param List<SetupHandler> pages
@param SetupHandler currentPage
@template.header(pages=pages,currentPage=currentPage)
<div class="card mt-3">
    <h4 class="card-header p-3">Security of setup</h4>
    <div class="card-body">
        <h4><i>etc/config.tdsl</i> configuration file is updated with new configuration. Please restart server to apply new configuration.</h4>
    </div>
</div>
@template.footer()