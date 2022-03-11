@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import tigase.http.modules.setup.pages.FeaturesPage
@import java.util.List
@param Config config
@param List<SetupHandler> pages
@param FeaturesPage currentPage
@template.header(pages=pages,currentPage=currentPage)
<script>
	var checkACS = function() {
		var acsRequired = false;
		["pubsub","muc","mix"].forEach(function(name) {
			var el = document.getElementById(name);
			if (el != null) {
				acsRequired |= el.checked || false;
			}
		});
		document.getElementById("acsComponent").required = acsRequired;
		document.getElementById("acsComponent").disabled = !document.getElementById("clusterMode").checked || (document.getElementById("clusterMode").checked && acsRequired);

		if (document.getElementById("acsComponent").checked) {
			document.getElementById("acsComponent").checked = document.getElementById("clusterMode").checked;
		} else {
			document.getElementById("acsComponent").checked = document.getElementById("clusterMode").checked && acsRequired;
		}
		document.getElementById("acs-warn").style.display = (document.getElementById("clusterMode").checked && acsRequired) ? "inline" : "none";
	};
	var checkNotes = function(id) {
		var noteEl = document.getElementById(id + "-note");
		if (noteEl != null) {
			noteEl.style.display = document.getElementById(id).checked ? "inline" : "none";
		}
	};
</script>
<form class="form-horizontal" method='POST'>
	<div class="card mt-3">
		<div class="card-body">
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="clusterMode" name="clusterMode" checked="${config.isClusterMode()}" onchange="checkACS()">
						<label class="form-check-label" for="clusterMode">Do you want your server to run in the cluster mode?</label>
					</div>
				</div>
			</div>
			@if(config.installationContainsACS())
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="acsComponent" name="acs" checked="${config.isACSEnabled()}">
						<label class="form-check-label" for="acs">Tigase Advanced Clustering Strategy (ACS) Component</label>
					</div>
					<div id="acs-warn" style="display: none;">
						<div class="alert alert-danger">
							Warning: Tigase ACS is required for PubSub, MUC and MIX components to work and cannot be disabled!
						</div>
					</div>
				</div>
			</div>
			@endif
		</div>
	</div>
	<div class="card mt-3">
		<div class="card-body">
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="muc" name="muc" checked="${config.isFeatureEnabled("muc")}" onchange="checkACS()">
						<label class="form-check-label" for="muc">Multi User Chat</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="pubsub" name="pubsub" checked="${config.isFeatureEnabled("pubsub")}" onchange="checkACS()">
						<label class="form-check-label" for="pubsub">Publish-Subscribe</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="mix" name="mix" checked="${config.isFeatureEnabled("mix")}" onchange="checkACS()">
						<label class="form-check-label" for="mix">Mediated Information eXchange</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="mam" name="mam" checked="${config.isFeatureEnabled("mam")}">
						<label class="form-check-label" for="mam">Message Archive</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="push" name="push" checked="${config.isFeatureEnabled("push")}">
						<label class="form-check-label" for="push">PUSH Notifications</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="upload" name="upload" checked="${config.isFeatureEnabled("upload")}">
						<label class="form-check-label" for="upload">HTTP File Upload</label>
					</div>
					<div id="upload-note" style="display: none;">
						<div class="alert alert-warning">
							If your server is not accessible from the internet using its hostname it may require manual configuration, see <a target="_blank" href="https://docs.tigase.net/tigase-http-api/master-snapshot/Tigase_HTTP-API_Guide/html/#_logic">serverName</a> property documentation.
						</div>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="carbons" name="carbons" checked="${config.isFeatureEnabled("carbons")}">
						<label class="form-check-label" for="carbons">Message Carbons</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="csi" name="csi" checked="${config.isFeatureEnabled("csi")}">
						<label class="form-check-label" for="csi">Client State Indication</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="motd" name="motd" checked="${config.isFeatureEnabled("motd")}">
						<label class="form-check-label" for="motd">Message of a Day</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="lastActivity" name="lastActivity" checked="${config.isFeatureEnabled("lastActivity")}">
						<label class="form-check-label" for="lastActivity">Last Activity</label>
					</div>
				</div>
			</div>
			<div class="row mb-3">
				<div class="col-sm-3"></div>
				<div class="col-sm-9">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" id="spam" name="spam" checked="${config.isFeatureEnabled("spam")}">
						<label class="form-check-label" for="spam">SPAM Filter</label>
					</div>
				</div>
			</div>
		</div>
	</div>
	<div class="m-3">
		<button class="btn btn-secondary" name="submit" type="next">Next</button>
	</div>
</form>
<script>
	checkACS();
	checkNotes("upload");
</script>
@template.footer()