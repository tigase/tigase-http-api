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
	<meta name="viewport" content="width=device-width, initial-scale=0.86">
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
	<meta charset="UTF-8"/>
	<title>Tigase XMPP Server</title>
	<link rel="stylesheet" type="text/css" href="assets/css/bootstrap.min.css">
	<link rel="stylesheet" type="text/css" href="assets/css/custom.css">
	<link rel="stylesheet" type="text/css" href="assets/css/http-api-custom.css">
</head>
<body class="d-flex flex-column min-vh-100">
<noscript>
	<div style="border: 2px solid red; margin: 3px">
		<div style="color: red; font-weight: bold; padding: 10px "><span>WARNING:</span><span>Your browser does not support JavaScript or JavaScript is disabled.</span></div>
		<div style="font-size: 0.9em; padding: 5px 15px">Tigase XMPP Server uses JavaScript to validate chosen configuration options - it may not work properly without JavaScript and may result in an incorrect configuration.<br/>We strongly recommend changing web browser or enabling JavaScript in web browser configuration.</div>
	</div>
</noscript>
<nav class="navbar navbar-expand-md navbar-dark" role = "navigation" style="margin-bottom: 0;">
	<div class="navbar-header">
		<a class="navbar-brand" href="/" id="logo">
			<p>Tigase XMPP Server</p>
		</a>
	</div>
</nav>
<main class="container-fluid p-0 d-flex flex-grow-1" role="main">
	<div class="sidebar d-flex" >
		<div class="accordion accordion-flush ps-0 w-100" id="accordion-commands">
			@for(String group : commandGroups)
				<div class="accordion-item">
					<h2 class="accordion-header" id="${group}-headers">
						<button class="accordion-button align-items-center ps-3 pe-3 @if(!group.equals(currentGroup))collapsed@endif" data-bs-toggle="collapse" data-bs-target="#${group}-collapse" aria-expanded="false" aria-controls="${group}-collapse">${group}</button>
					</h2>
					<div id="${group}-collapse" class="accordion-collapse collapse ps-3 pe-3 @if(group.equals(currentGroup))show@endif" aria-labelledby="${group}-headers" data-bs-parent="#accordion-commands">
						<div class="accordion-body ps-3">
							<ul class="btn-toggle-nav list-unstyled fw-normal pb-1">
								@for(CommandItem command : commands.get(group))
									<li class="@if(command.equals(currentCommand))active@endif">
										<a href="?_group=${java.net.URLEncoder.encode(group,StandardCharsets.UTF_8)}&_jid=${java.net.URLEncoder.encode(command.getJid().toString(),StandardCharsets.UTF_8)}&_node=${java.net.URLEncoder.encode(command.getNode(),StandardCharsets.UTF_8)}" class="d-block command-name">${command.getName()}</a>
										<span class="d-block command-jid">${command.getJid().toString()}</span>
									</li>
								@endfor
							</ul>
						</div>
					</div>
				</div>
			@endfor
		</div>
	</div>
	<div class="flex-grow-1 d-flex flex-column">
		<div class="ps-4 pe-4">
			@if(form != null)
				<form method="POST" class="form-horizontal m-3">
					<div class="card">
						@if(form.getTitle() != null)
							<h5 class="card-header p-3">${form.getTitle()}</h5>
						@endif
						<div class="card-body">
							@if(form.getInstructions() != null)
								<h5>${form.getInstructions()}</h5>
							@endif
							@for(Reported reported : form.getReported())
								<div class="card">
									@if(reported.getLabel() != null)
										<h5 class="card-header">${ reported.getLabel() }></h5>
									@endif
									<div class="card-body">
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
								</div>
							@endfor
							@for(Field formField : form.getFields())
								!{ var type = formField.getType(); }
								@if(type == "hidden")
									<input type='hidden' name="${formField.getVar()}" value="${formField.getValue()}"/><br/>
								@elseif(type == "boolean")
									<div class="row m-3">
										<div class="offset-sm-3 col-sm-9">
											<input class="form-check-input" type="checkbox" id="${formField.getVar()}" name='${formField.getVar()}'
												   checked="${formField.isChecked()}"
												   required="${formField.isRequired()}">
											<label class="form-check-label" for="${formField.getVar()}">${ formField.getLabel() }</label>
										</div>
										@if(formField.getDesc() != null)
											<small class="text-muted offset-sm-3 col-sm-9">${formField.getDesc()}</small>
										@endif
									</div>
								@elseif(type == "text-private")
									<div class="row m-3">
										<label class="col-sm-3 col-form-label"
											   for='${formField.getVar()}'>${ formField.getLabel() }</label>
										<div class="col-sm-9">
											<input class="form-control" type='password' size='90'
												   name='${formField.getVar()}' value="${formField.getValue()}"
												   required="${formField.isRequired()}"/>
										</div>
										@if(formField.getDesc() != null)
											<small class="text-muted offset-sm-3 col-sm-9">${formField.getDesc()}</small>
										@endif
									</div>
								@elseif(type == "text-multi" || type == "jid-multi")
									<div class="row m-3">
										<label class="col-sm-3 col-form-label"
											   for='${formField.getVar()}'>${ formField.getLabel() }</label>
										<div class="col-sm-9">
										<textarea class="form-control" rows='5' name='${formField.getVar()}'
												  required="${formField.isRequired()}">${formField.getMultilineTextValue()}</textarea>
										</div>
										@if(formField.getDesc() != null)
											<small class="text-muted offset-sm-3 col-sm-9">${formField.getDesc()}</small>
										@endif
									</div>
								@elseif(type == "list-single" || type == "list-multi")
									<div class="row m-3">
										<label class="col-sm-3 col-form-label"
											   for='${formField.getVar()}'>${ formField.getLabel() }</label>
										<div class="col-sm-9">
											<select class="form-control" name='${formField.getVar()}'
													multiple="${formField.isMultiple()}"
													required="${formField.isRequired()}">
												@for(Field.Option option : formField.getOptions())
													@if(option.getValue() != null)
														<option value="${option.getValue()}"
																selected="${formField.isSelected(option)}">${option.getLabel()}</option>
													@else
														<option value=""
																selected="${formField.isSelected(option)}">${option.getLabel()}</option>
													@endif
												@endfor
											</select>
										</div>
										@if(formField.getDesc() != null)
											<small class="text-muted offset-sm-3 col-sm-9">${formField.getDesc()}</small>
										@endif
									</div>
								@elseif(type == "text-single" || type == "jid-single")
									@if("datetime" == formField.getSubType())
										<div class="row m-3">
											<label class="col-sm-3 col-form-label"
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
												<small class="text-muted offset-sm-3 col-sm-9">${formField.getDesc()}</small>
											@endif
										</div>
									@else
										<div class="row m-3">
											<label class="col-sm-3 col-form-label"
												   for='${formField.getVar()}'>${formField.getLabel()}</label>
											<div class="col-sm-9">
												<input class="form-control" type='text' size='90'
													   name='${formField.getVar()}' value="${formField.getValue()}"
													   required="${formField.isRequired()}"/>
											</div>
											@if(formField.getDesc() != null)
												<small class="text-muted offset-sm-3 col-sm-9">${formField.getDesc()}</small>
											@endif
										</div>
									@endif
								@elseif(type == "fixed")
									<div class="row m-3">
										<label class="col-sm-3 col-form-label"
											   for='${formField.getVar()}'>${ formField.getLabel() }</label>
										<div class="col-sm-9">
											<input class="form-control" disabled type='text' size='90'
												   name='${formField.getVar()}' value="${formField.getValue()}"
												   required="${formField.isRequired()}"/>
										</div>
										@if(formField.getDesc() != null)
											<small class="text-muted offset-sm-3 col-sm-9">${formField.getDesc()}</small>
										@endif
									</div>
								@endif
							@endfor
						</div>
					</div>
					<div class="m-3">
						<button class="btn btn-secondary" name="submit" type="submit">Submit</button>
					</div>
				</form>
			@else
				<div class="m-5 text-center">
					<h2>Welcome to <span style="font-size: 1.25em;">Tigase XMPP Server</span> admin panel</h2>
				</div>
				<div class="card m-4">
					<div class="card-header">First steps</div>
					<div class="card-body">
						<ul>
							<li>Main configuration categories are located on the left.</li>
							<li>After expanding them you can see a list of items from the group.</li>
							<li>Each item has name (upper line) and associated component (lower line).</li>
							<li>For more details please refer to the documentation included in the distribution archive or <a
										href="https://docs.tigase.net/">online</a></li>
						</ul>
					</div>
				</div>
				<div class="card m-4">
					<div class="card-header">Most common actions</div>
					<div class="card-body">
						<ul>
							@for(CommandItem command : defaultCommands)
								<li>
									<a href="?_group=${java.net.URLEncoder.encode(command.getGroup(),StandardCharsets.UTF_8)}&_jid=${java.net.URLEncoder.encode(command.getJid().toString(),StandardCharsets.UTF_8)}&_node=${java.net.URLEncoder.encode(command.getNode(),StandardCharsets.UTF_8)}">${command.getName()}</a>
								</li>
							@endfor
						</ul>
					</div>
				</div>
			@endif
		</div>
		<footer class="footer mt-auto">
			<div class="container-fluid d-flex flex-row-reverse">
				<p class="text-secondary">Powered by Tigase XMPP Server ${tigase.server.XMPPServer.getImplementationVersion()}</p>
			</div>
		</footer>
	</div>
</main>
<script src="assets/js/bootstrap.bundle.min.js"></script>
</body>
</html>