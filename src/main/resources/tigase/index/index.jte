@import tigase.http.DeploymentInfo
@import java.util.List
@param List<DeploymentInfo> deployments
<!DOCTYPE html>
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=0.86">
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
	<meta charset="UTF-8"/>
	<title>Tigase XMPP Server</title>
	<link rel="stylesheet" type="text/css" href="assets/css/bootstrap.min.css">
	<link rel="stylesheet" type="text/css" href="assets/css/custom.css">
	<link rel="stylesheet" type="text/css" href="assets/css/http-api-custom.css">
</head>
<body class="d-flex flex-column h-100">
<noscript>
	<div style="border: 2px solid red; margin: 3px">
		<div style="color: red; font-weight: bold; padding: 10px "><span>WARNING:</span><span>Your browser does not support JavaScript or JavaScript is disabled.</span></div>
		<div style="font-size: 0.9em; padding: 5px 15px">Tigase XMPP Server Setup uses JavaScript to validate chosen configuration options - it may not work properly without JavaScript and may result in an incorrect configuration.<br/>We strongly recommend changing web browser or enabling JavaScript in web browser configuration.</div>
	</div>
</noscript>
<nav class="navbar navbar-expand-md navbar-dark" role = "navigation" style="margin-bottom: 0;">
	<div class="navbar-header">
		<a class="navbar-brand" href="/" id="logo">
			<p>Tigase XMPP Server</p>
		</a>
	</div>
</nav>
<main class="container" style="max-width: 85%;" role="main">
	<div class="row">
		<div class="card panel-default mb-4">
			<div class="card-body">
				<h5 class="card-title">List of available endpoints for this server instance</h5>
			</div>
		</div>
	</div>
		<div class="row">
			@for(DeploymentInfo info : deployments)
				<div class="col-sm-12 col-xl-6 p-2">
					<div class="card panel-primary">
						<div class="card-body">
							<h5 class="card-title">${info.getDeploymentName()}</h5>
							<p class="card-text">${info.getDeploymentDescription()}</p>
							<a href="${info.getContextPath()}/" class="btn btn-tigase text-light">Go to ${info.getContextPath()}</a>
						</div>
					</div>
				</div>
			@endfor
		</div>
</main>
<footer class="footer mt-auto">
	<div class="container-fluid d-flex flex-row-reverse">
		<p class="text-secondary">Powered by Tigase XMPP Server ${tigase.server.XMPPServer.getImplementationVersion()}</p>
	</div>
</footer>
</body>
</html>