@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import tigase.http.modules.setup.pages.BasicConfigPage
@import tigase.http.modules.setup.pages.ConnectivityPage
@import tigase.kernel.beans.selector.ConfigTypeEnum
@import tigase.db.util.SchemaLoader.TypeInfo
@import tigase.xmpp.jid.BareJID
@import java.util.List
@import java.util.stream.Collectors
@param Config config
@param List<SetupHandler> pages
@param ConnectivityPage currentPage
@template.layout(pages = pages, currentPage = currentPage, content = @`
<form class="form-horizontal" method='POST'>
    <div class="card mt-3">
        <div class="card-body">
            <div class="row mb-3">
                <div class="col-sm-3"></div>
                <div class="col-sm-9">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="c2s" name="c2s" checked="${config.isConnectorEnabled("c2s")}">
                        <label class="form-check-label" for="c2s">Desktop / Mobile</label>
                    </div>
                </div>
            </div>
            <div class="row mb-3">
                <div class="col-sm-3"></div>
                <div class="col-sm-9">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="bosh" name="bosh" checked="${config.isConnectorEnabled("bosh")}">
                        <label class="form-check-label" for="bosh">Web (HTTP)</label>
                    </div>
                </div>
            </div>
            <div class="row mb-3">
                <div class="col-sm-3"></div>
                <div class="col-sm-9">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="ws2s" name="ws2s" checked="${config.isConnectorEnabled("ws2s")}">
                        <label class="form-check-label" for="ws2s">Web (WebSocket)</label>
                    </div>
                </div>
            </div>
            <div class="row mb-3">
                <div class="col-sm-3"></div>
                <div class="col-sm-9">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="s2s" name="s2s" checked="${config.isConnectorEnabled("s2s")}">
                        <label class="form-check-label" for="s2s">Federation</label>
                    </div>
                </div>
            </div>
            <div class="row mb-3">
                <div class="col-sm-3"></div>
                <div class="col-sm-9">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="ext" name="ext" checked="${config.isConnectorEnabled("ext")}">
                        <label class="form-check-label" for="ext">Web (WebSocket)</label>
                    </div>
                </div>
            </div>
            <div class="row mb-3">
                <div class="col-sm-3"></div>
                <div class="col-sm-9">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="http" name="http" checked="${config.isConnectorEnabled("http")}">
                        <label class="form-check-label" for="http">REST API / Admin UI</label>
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