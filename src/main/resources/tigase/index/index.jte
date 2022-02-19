@import tigase.http.DeploymentInfo
@import java.util.List
@param List<DeploymentInfo> deployments
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
	<meta charset="UTF-8"/>
	<title>Tigase XMPP Server</title>
	<link rel="stylesheet" type="text/css" href="assets/css/bootstrap.min.css">
	<link rel="stylesheet" type="text/css" href="assets/css/custom.css">
	<link rel="stylesheet" type="text/css" href="assets/css/http-api-custom.css">
</head>
<body>
<noscript>
	<div style="border: 2px solid red; margin: 3px">
		<div style="color: red; font-weight: bold; padding: 10px "><span>WARNING:</span><span>Your browser does not support JavaScript or JavaScript is disabled.</span></div>
		<div style="font-size: 0.9em; padding: 5px 15px">Tigase XMPP Server Setup uses JavaScript to validate chosen configuration options - it may not work properly without JavaScript and may result in an incorrect configuration.<br/>We strongly recommend changing web browser or enabling JavaScript in web browser configuration.</div>
	</div>
</noscript>
<div id="wrapper">
	<nav class="navbar navbar-default navbar-static-top" role = "navigation" style="margin-bottom: 0;">
		<div class="navbar-header">
			<a class="navbar-brand" href="/" id="logo">
				<p>Tigase XMPP Server</p>
			</a>
		</div>
	</nav>
	<div id="page-wrapper" class="no-sidebar"}>
	<div class="row">
		<div class="col-sm-12 col-md-12 col-lg-12">
			<div class="panel panel-default">
				<div class="panel-heading panel-xs">
					<h4>List of available endpoints for this server instance</h4>
				</div>
			</div>

			@for(DeploymentInfo info : deployments)
			<div class="col-sm-6 col-md-6 col-lg-6">
				<a href="${info.getContextPath()}/">
			<div class="panel panel-primary">
				<div class="panel-heading panel-xs">
					<span style="float: left;"><h4 style="display: inline;">${info.getContextPath()}</h4></span><span style="float: right;"><h4 style="display: inline;">${info.getDeploymentName()}</h4></span>
					<div style="clear: both;"></div>
				</div>
				<div class="panel-body">
					<h4 style="text-decoration: none;">${info.getDeploymentDescription()}</h4>
				</div>
			</div>
				</a>
			</div>
			@endfor
			</div>
		</div>
	</div>
</div>
<div id="footer" class="container-fluid">
	<div class="row">
		<div class="panel-footer text-right">
			<p>Powered by Tigase XMPP Server ${tigase.server.XMPPServer.getImplementationVersion()}</p>
		</div>
	</div>
</div>
</body>
</html>