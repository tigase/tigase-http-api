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
        <h4 class="card-header p-3">VoIP configuration</h4>
        <div class="card-body">
            <div class="row mb-3">
                <p>By default Tigase XMPP Server is allows VoIP communication over XMPP.</p>
                <p>To improve connectivity and reliability of VoIP connections, it is suggested to provide Tigase XMPP Server with TURN server connectivity details.</p>
                <p>Those details will be shared with XMPP clients that will have accounts on your server, allowing them to use TURN server for VoIP connections</p>
            </div>
            <div class="row mb-3">
                <label class="col-sm-3 col-form-label" for="type">Type</label>
                <div class="col-sm-9">
                    <select id="type" class="form-select" name="type" required>
                        <option selected="${config.getVoipConfig().getType().name().equals("turn")}" value="turn">TURN</option>
                        <option selected="${config.getVoipConfig().getType().name().equals("turns")}" value="turns">TURNS</option>
                        <option selected="${config.getVoipConfig().getType().name().equals("stun")}" value="stun">STUN</option>
                        <option selected="${config.getVoipConfig().getType().name().equals("stuns")}" value="stuns">STUNS</option>
                    </select>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-3 col-form-label" for="host">Host</label>
                <div class="col-sm-9">
                    <input id="host" class="form-control" name="host" type="text" required size="6s0" value="${config.getVoipConfig().getHost()}" />
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-3 col-form-label" for="port">Port</label>
                <div class="col-sm-9">
                    <input id="port" class="form-control" name="port" type="number" required value="${config.getVoipConfig().getPort()}" />
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-3 col-form-label" for="transport">Transport</label>
                <div class="col-sm-9">
                    <select id="transport" class="form-select" name="transport">
                        <option selected="${config.getVoipConfig().getTransport() == null}" value="">None</option>
                        <option selected="${config.getVoipConfig().getTransport() != null && config.getVoipConfig().getTransport().name().equals("tcp")}" value="tcp">TCP</option>
                        <option selected="${config.getVoipConfig().getTransport() != null && config.getVoipConfig().getTransport().name().equals("udp")}" value="udp">UDP</option>
                    </select>
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-3 col-form-label" for="username">Username</label>
                <div class="col-sm-9">
                    <input id="username" class="form-control" name="username" type="text" size="30" value="${config.getVoipConfig().getUsername()}" />
                </div>
            </div>
            <div class="row mb-3">
                <label class="col-sm-3 col-form-label" for="password">Password</label>
                <div class="col-sm-9">
                    <input id="password" class="form-control" name="password" type="password" size="30" value="${config.getVoipConfig().getPassword()}"/>
                </div>
            </div>
        </div>
    </div>
    <div class="m-3">
        <button class="btn btn-secondary" name="submit" type="next">Next</button>
    </div>
</form>
`)