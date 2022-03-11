@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import java.util.List
@param Config config
@param List<SetupHandler> pages
@param SetupHandler currentPage
@template.header(pages=pages,currentPage=currentPage)
<form class="form-horizontal" method='POST'>
    <div class="card mt-3">
        <div class="card-body">
            <p>While the Tigase server is quite stable and well tested application the installer itself is a new addition. Take precautions especially if you upgrade the server from earlier version. Backup old server files and the database.</p>
            <p>If you notice any problems please report them at Tigase XMPP Server <a href="https://projects.tigase.org/projects/tigase-server/boards">forum</a> or create a new <a href="https://projects.tigase.org/projects/tigase-server/issues/new">issue</a>.</p>
        </div>
    </div>
    <div class="m-3">
        <button class="btn btn-secondary" name="submit" type="next">Next</button>
    </div>
</form>
@template.footer()