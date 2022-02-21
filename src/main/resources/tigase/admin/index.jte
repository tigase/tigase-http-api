@import tigase.http.modules.admin.form.Field
@import tigase.http.modules.admin.form.Form
@import tigase.http.modules.admin.form.Reported
@import tigase.xml.Element
@import java.nio.charset.StandardCharsets
@import java.time.ZoneId
@import java.util.Map
@import java.util.List
@import tigase.http.modules.admin.CommandItem
@param String currentGroup
@param CommandItem currentCommand
@param Map<String,List<CommandItem>> commands
@param List<String> commandGroups
@param List<CommandItem> defaultCommands
@param Form form
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/html">
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
		<div style="font-size: 0.9em; padding: 5px 15px">Tigase XMPP Server uses JavaScript to validate chosen configuration options - it may not work properly without JavaScript and may result in an incorrect configuration.<br/>We strongly recommend changing web browser or enabling JavaScript in web browser configuration.</div>
	</div>
</noscript>
<div id="wrapper">
	<nav class="navbar navbar-default navbar-static-top" role = "navigation" style="margin-bottom: 0;">
		<div class="navbar-header">
			<a class="navbar-brand" href="/admin/" id="logo">
				<p>Tigase XMPP Server - Admin console@if(currentCommand != null): ${currentCommand.getName()}@endif</p>
			</a>
		</div>
		<div class="navbar-default sidebar" role="navigation">
			<div class="sidebar-nav navbar-collapse">
				<ul class="nav" id="side-menu">
					@for(String group : commandGroups)
						<li>
							<a class="sidebar-group-header@if(group.equals(currentGroup)) active@endif" href="?_group=${java.net.URLEncoder.encode(group,StandardCharsets.UTF_8)}"><i class="fa fa-fw"></i>${group}</a>
							@if(group.equals(currentGroup))
							<ul>
								@for(CommandItem command : commands.get(group))
								<li class="@if(command.equals(currentCommand))active@endif"><a class="command-name" href="?_group=${java.net.URLEncoder.encode(group,StandardCharsets.UTF_8)}&_jid=${java.net.URLEncoder.encode(command.getJid().toString(),StandardCharsets.UTF_8)}&_node=${java.net.URLEncoder.encode(command.getNode(),StandardCharsets.UTF_8)}">${command.getName()}</a><br/><span class="command-jid">${command.getJid().toString()}</span></li>
								@endfor
							</ul>
							@endif
						</li>
					@endfor
				</ul>
			</div>
		</div>
	</nav>
	<div id="page-wrapper">
		<div class="row">
		<div class="col-sm-12 col-md-12 col-lg-12">
			@if(form != null)
			<form method="POST" class="form-horizontal">
				<div class="panel panel-default">
					@if(form.getTitle() != null)
					<div class="panel-heading panel-xs">
						<h4>${form.getTitle()}</h4>
					</div>
					@endif
					<div class="panel-body">
						@if(form.getInstructions() != null)
						<h5>${form.getInstructions()}</h5>
						@endif
						@for(Reported reported : form.getReported())
							<div class="panel panel-default">
								@if(reported.getLabel() != null)
									<div class="panel-heading"><h5 class="panel-title">${ reported.getLabel() }></h5>
									</div>
								@endif
								<table class="table">
									<thead>
									<tr>
										@for(Reported.FieldDefinition field : reported.getFields())
											<th>${field.getLabel()}</th>
										@endfor
									</tr>
									</thead>
									<tbody>
									@for(Reported.Item item : reported.getItems())
										<tr>
											@for(Reported.FieldDefinition fieldDef : reported.getFields())
												<td style="${fieldDef.getStyle()}">${item.getField(fieldDef.getVar()).getValueLabel()}</td>
											@endfor
										</tr>
									@endfor
									</tbody>
								</table>
							</div>
						@endfor
						@for(Field formField : form.getFields())
							!{ var type = formField.getType(); }
							@if(type == "hidden") {
								<input type='hidden' name="${formField.getVar()}" value="${formField.getValue()}"/><br/>
							@elseif(type == "boolean")
								<div class="form-group">
									<div class="col-sm-offset-3 col-sm-9">
										<div class="checkbox">
											<label>
												<input type="checkbox" name='${formField.getVar()}'
													   checked="${formField.isChecked()}"
													   required="${formField.isRequired()}">${ formField.getLabel() }
											</label>
										</div>
									</div>
									@if(formField.getDesc() != null)
										<small class="text-muted col-sm-offset-3 col-sm-9">${formField.getDesc()}</small>
									@endif
								</div>
							@elseif(type == "text-private")
								<div class="form-group">
									<label class="col-sm-3 control-label"
										   for='${formField.getVar()}'>${ formField.getLabel() }</label>
									<div class="col-sm-9">
										<input class="form-control" type='password' size='90'
											   name='${formField.getVar()}' value="${formField.getValue()}"
											   required="${formField.isRequired()}"/>
									</div>
									@if(formField.getDesc() != null)
										<small class="text-muted col-sm-offset-3 col-sm-9">${formField.getDesc()}</small>
									@endif
								</div>
							@elseif(type == "text-multi" || type == "jid-multi")
								<div class="form-group">
									<label class="col-sm-3 control-label"
										   for='${formField.getVar()}'>${ formField.getLabel() }</label>
									<div class="col-sm-9">
										<textarea class="form-control" rows='5' name='${formField.getVar()}'
												  required="${formField.isRequired()}">${formField.getMultilineTextValue()}</textarea>
									</div>
									@if(formField.getDesc() != null)
										<small class="text-muted col-sm-offset-3 col-sm-9">${formField.getDesc()}</small>
									@endif
								</div>
							@elseif(type == "list-single" || type == "list-multi")
								<div class="form-group">
									<label class="col-sm-3 control-label"
										   for='${formField.getVar()}'>${ formField.getLabel() }</label>
									<div class="col-sm-9">
										<select class="form-control" name='${formField.getVar()}'
												multiple="${formField.isMultiple()}"
												required="${formField.isRequired()}">
											@for(Field.Option option : formField.getOptions())
												<option value='${option.getValue()}'
														selected="${formField.isSelected(option)}">${option.getLabel()}</option>
											@endfor
										</select>
									</div>
									@if(formField.getDesc() != null)
										<small class="text-muted col-sm-offset-3 col-sm-9">${formField.getDesc()}</small>
									@endif
								</div>
							@elseif(type == "text-single" || type == "jid-single")
								@if("datetime" == formField.getSubType())
									<div class="form-group">
										<label class="col-sm-3 control-label"
											   for='${formField.getVar()}'>${ formField.getLabel() }</label>
										<div class="col-sm-3">
											<input class="form-control" type='date' name='${formField.getVar()}_date'
												   value="${formField.getDateString()}" required="${formField.isRequired()}"/>
										</div>
										<div class="col-sm-2">
											<input class="form-control" type='time' name='${formField.getVar()}_time'
												   value="${formField.getTimeString()}" required="${formField.isRequired()}"/>
										</div>
										<div class="col-sm-3">
											<select class="form-control" name='${formField.getVar()}_tz'
													required="${formField.isRequired()}">
												@for(String zoneId : java.util.TimeZone.getAvailableIDs())
													<option selected="${formField.getTimeZone() == java.util.TimeZone.getTimeZone(zoneId)}">${ zoneId }</option>
												@endfor
											</select>
										</div>
										@if(formField.getDesc() != null)
											<small class="text-muted col-sm-offset-3 col-sm-9">${formField.getDesc()}</small>
										@endif
									</div>
								@else
									<div class="form-group">
										<label class="col-sm-3 control-label"
											   for='${formField.getVar()}'>${formField.getLabel()}</label>
										<div class="col-sm-9">
											<input class="form-control" type='text' size='90'
												   name='${formField.getVar()}' value="${formField.getValue()}"
												   required="${formField.isRequired()}"/>
										</div>
									</div>
								@endif
							@elseif(type == "fixed") { %>
								<div class="form-group">
									<label class="col-sm-3 control-label"
										   for='${formField.getVar()}'>${ formField.getLabel() }</label>
									<div class="col-sm-9">
										<input class="form-control" disabled type='text' size='90'
											   name='${formField.getVar()}' value="${formField.getValue()}"
											   required="${formField.isRequired()}"/>
									</div>
									@if(formField.getDesc() != null)
										<small class="text-muted col-sm-offset-3 col-sm-9">${formField.getDesc()}</small>
									@endif
								</div>
							@endif
						@endfor
					</div>
				</div>
				<input class="btn btn-default" name="submit" type="submit"/>
			</form>
			@else
			<div class="page-header text-center">
				<h1>Welcome to <big>Tigase XMPP Server</big> admin panel</h1>
			</div>
			<div class="panel panel-default">
				<div class="panel-heading">
					<h1 class="panel-title">First steps</h1>
				</div>
				<div class="panel-body lead">
					<ul>
						<li>Main configuration categories are located on the left.</li>
						<li>After expanding them you can see a list of items from the group.</li>
						<li>Each item has name (upper line) and associated component (lower line).</li>
						<li>For more details please refer to the documentation included in the distribution archive or <a href="https://docs.tigase.net/">online</a> </li>
					</ul>
				</div>
			</div>
			<div class="panel panel-default">
				<div class="panel-heading">
					<h1 class="panel-title">Most common actions</h1>
				</div>
				<div class="panel-body lead">
					<ul>
						@for(CommandItem command : defaultCommands)
							<li><a href="?_group=${java.net.URLEncoder.encode(command.getGroup(),StandardCharsets.UTF_8)}&_jid=${java.net.URLEncoder.encode(command.getJid().toString(),StandardCharsets.UTF_8)}&_node=${java.net.URLEncoder.encode(command.getNode(),StandardCharsets.UTF_8)}">${command.getName()}</a></li>
						@endfor
					</ul>
				</div>
			</div>
			@endif
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
</div>
</body>
</html>