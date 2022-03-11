@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import tigase.http.modules.setup.pages.BasicConfigPage
@import tigase.kernel.beans.selector.ConfigTypeEnum
@import tigase.db.util.SchemaLoader.TypeInfo
@import tigase.xmpp.jid.BareJID
@import java.util.List
@import java.util.stream.Collectors
@param Config config
@param List<SetupHandler> pages
@param BasicConfigPage currentPage
@template.header(pages=pages,currentPage=currentPage)
<form class="form-horizontal" method='POST'>
    <div class="card mt-3">
        <div class="card-body">
<p>On this panel you can specify basic configuration settings for the Tigase server.</p>
<p>Based on your selection here more configuration options might be presented later on. After the configuration is complete config.tdsl file will be created.</p>
<p>You can optionally restart the server at the end of the process if you like.</p>

<div class="row mb-3">
    <label class="col-sm-3 col-form-label" for="configType">Configuration type</label>
    <div class="col-sm-9">
        <select class="form-select" name="configType" required>
            @for(ConfigTypeEnum type : currentPage.getConfigTypes())
                <option value='${type.name()}' selected="${type == config.getConfigType()}" >${currentPage.getConfigTypeEnumLabel(type)}</option>
            @endfor
        </select>
    </div>
</div>

<div class="row mb-3">
    <label class="col-sm-3 col-form-label" for="virtualDomain">Your XMPP (Jabber) default domain</label>
    <div class="col-sm-9">
        <input class="form-control" name="virtualDomain" type="text" size="30" value="${config.getDefaultVirtualDomain()}" required/>
    </div>
</div>

<div class="row mb-3">
    <label class="col-sm-3 col-form-label" for="admins">Server administrators</label>
    <div class="col-sm-9">
        <input class="form-control" name="admins" type="text" size="30" value="${config.getAdmins().stream().map(BareJID::toString).collect(Collectors.joining(","))}" />
    </div>
</div>

<div class="row mb-3">
    <label class="col-sm-3 col-form-label" for="adminPwd">Admin password</label>
    <div class="col-sm-9">
        <input class="form-control" name="adminPwd" type="password" size="30" value="${config.getAdminPwd()}"/>
    </div>
</div>

<div class="row mb-3">
    <label class="col-sm-3 col-form-label" for="dbType">Select database</label>
    <div class="col-sm-9">
    <select class="form-select" id="dbType" name="dbType" required onchange="
	var select = document.getElementById('dbType');
	for (var i=0; i<select.children.length; i++) {
    var it = select.children[i];
    if (it.value == select.value) {
		var warn = document.getElementById('dbTypeWarning');
		warn.innerHTML = it.getAttribute('warning') || '';
	}
}
">
        @for(TypeInfo type : tigase.db.util.SchemaLoader.getAllSupportedTypes())
            <option value='${type.getName()}' selected="${type.isAvailable() && type == config.getDbType()}" disabled="${!type.isAvailable()}" warning="${type.getWarning()}" >${type.getLabel()}</option>
        @endfor
    </select>
    <div id='dbTypeWarning' class="dbTypeWarning"></div>
    </div>
</div>
</div>
</div>
    <div class="m-3">
        <button class="btn btn-secondary" name="submit" type="next">Next</button>
    </div>

</form>
@template.footer()
