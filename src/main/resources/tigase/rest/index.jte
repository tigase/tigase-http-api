@import gg.jte.support.ForSupport
@import io.swagger.v3.oas.annotations.responses.ApiResponse
@import tigase.http.modules.rest.RestServlet
@import tigase.http.modules.rest.docs.Endpoint
@import tigase.http.modules.rest.docs.Parameter
@import tigase.http.modules.rest.docs.Response
@import java.util.List
@import java.util.stream.Collectors
@param List<Endpoint> endpoints
@param String restBasePath
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
    <link rel="stylesheet" type="text/css" href="assets/css/prism.css">
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
<main class="container" role="main">
    <div class="row">
        <div class="card panel-default mb-4">
            <div class="card-body">
                <h5 class="card-title">List of available endpoints for this server instance</h5>
            </div>
        </div>
    </div>
    @for(Endpoint endpoint : endpoints)
    <div class="row mt-1 mb-1">
        <div class="col-sm-12">
            <div class="card endpoint method-${endpoint.getMethod().name()} bg-opacity-10">
                <a data-bs-toggle="collapse" class="card-header d-flex align-items-center text-decoration-none text-dark w-100 collapsed" href="#collapse-${endpoint.getId()}" role="button" aria-expanded="false" aria-controls="collapse-${endpoint.getId()}">
                    <div class="method method-${endpoint.getMethod().name()} bg-opacity-100 text-light rounded-3 me-2">${endpoint.getMethod().name()}</div>
                    <div class="path me-2">${endpoint.getPath()}</div>
                    <div class="description">${endpoint.getName()}</div>
                    <div class="collapse-button"></div>
                </a>
                <div class="card-body collapse" id="collapse-${endpoint.getId()}">
                    <p class="card-text">${endpoint.getDescription()}</p>
                    @if(!endpoint.getParameters().isEmpty())
                        <table class="table table-sm caption-top">
                            <caption>Parameters</caption>
                            <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Description</th>
                                </tr>
                            </thead>
                            <tbody>
                            @for(Parameter param : endpoint.getParameters())
                                <tr>
                                    <td style="width: 10em;">
                                        <div class="parameter name d-flex align-items-top text-nowrap">${param.getName()}@if(param.isRequired())<span class="parameter required">* required</span>@endif</div>
                                        <div class="parameter type">${param.getType()}</div>
                                        <div class="parameter kind text-muted fst-italic">(${param.getKind().name()})</div>
                                    </td>
                                    <td class="parameter">
                                        @if(param.getDescription() != null && !param.getDescription().isEmpty())<p class="description">${param.getDescription()}</p>@endif
                                        @if(param.getPossibleValues() != null)<p class="possible-values">Available values: ${param.getPossibleValues().stream().collect(Collectors.joining(", "))}</p>@endif
                                        @if(!param.getPayloadExamples().isEmpty())
                                            <ul class="nav nav-pills" style="font-size: 0.85em" data-bs-tabs="tabs">
                                                @for(var entry : ForSupport.of(param.getPayloadExamples()))
                                                    !{var example = entry.get(); }
                                                    <li class="nav-item">
                                                        <a class="nav-link@if(entry.isFirst()) active@endif" aria-current="${entry.isFirst()}" data-bs-toggle="tab" href="#${example.getId()}">${example.getContentType()}</a>
                                                    </li>
                                                @endfor
                                                @if(param.getModel() != null)
                                                    <li class="nav-item">
                                                        <a class="nav-link@if(param.getPayloadExamples().isEmpty()) active@endif" aria-current="${param.getPayloadExamples().isEmpty()}" data-bs-toggle="tab" href="#${param.getModel().getId()}">Model</a>
                                                    </li>
                                                @endif
                                            </ul>
                                            <div class="tab-content" style="display: block; margin-top: -7px; margin-bottom: -7px; font-size: 0.85em">
                                                @for(var entry : ForSupport.of(param.getPayloadExamples()))
                                                    !{var example = entry.get(); }
                                                    !{var lang = example.getContentType().contains("json") ? "javascript" : "xml"; }
                                                    <div class="tab-pane fade @if(entry.isFirst()) active show@endif" id="${example.getId()}">
                                                        <pre><code class="language-${lang}">${example.getPayload()}</code></pre>
                                                    </div>
                                                @endfor
                                                @if(param.getModel() != null)
                                                    <div class="tab-pane fade" id="${param.getModel().getId()}">
                                                        @template.model(model = param.getModel())
                                                    </div>
                                                @endif
                                            </div>
                                        @endif
                                    </td>
                                </tr>
                            @endfor
                            </tbody>
                        </table>
                    @endif
                    @if(!endpoint.getResponses().isEmpty())
                        <table class="table table-sm caption-top">
                            <caption>Responses</caption>
                            <thead>
                                <tr>
                                    <th>Code</th>
                                    <th>Description</th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(Response response : endpoint.getResponses())
                                    <tr>
                                        <td style="width: 10em;">${response.getCode()}</td>
                                        <td class="response">
                                            @if(response.getDescription() != null && !response.getDescription().isEmpty())<p class="description">${response.getDescription()}</p>@endif
                                            @if(!response.getPayloadExamples().isEmpty())
                                                <ul class="nav nav-pills" style="font-size: 0.85em" data-bs-tabs="tabs">
                                                    @for(var entry : ForSupport.of(response.getPayloadExamples()))
                                                        !{var example = entry.get(); }
                                                        <li class="nav-item">
                                                            <a class="nav-link@if(entry.isFirst()) active@endif" aria-current="${entry.isFirst()}" data-bs-toggle="tab" href="#${example.getId()}">${example.getContentType()}</a>
                                                        </li>
                                                    @endfor
                                                    @if(response.getModel() != null)
                                                    <li class="nav-item">
                                                        <a class="nav-link@if(response.getPayloadExamples().isEmpty()) active@endif" aria-current="${response.getPayloadExamples().isEmpty()}" data-bs-toggle="tab" href="#${response.getModel().getId()}">Model</a>
                                                    </li>
                                                    @endif
                                                </ul>
                                                <div class="tab-content" style="display: block; margin-top: -7px; margin-bottom: -7px; font-size: 0.85em">
                                                    @for(var entry : ForSupport.of(response.getPayloadExamples()))
                                                        !{var example = entry.get(); }
                                                        !{var lang = example.getContentType().contains("json") ? "javascript" : "xml"; }
                                                        <div class="tab-pane fade @if(entry.isFirst()) active show@endif" id="${example.getId()}">
                                                            <pre><code class="language-${lang}">${example.getPayload()}</code></pre>
                                                        </div>
                                                    @endfor
                                                    @if(response.getModel() != null)
                                                    <div class="tab-pane fade" id="${response.getModel().getId()}">
                                                    @template.model(model = response.getModel())
                                                    </div>
                                                    @endif
                                                </div>
                                            @endif
                                        </td>
                                    </tr>
                                @endfor
                            </tbody>
                        </table>
                    @endif
                </div>
            </div>
        </div>
    </div>
    @endfor
</main>
<footer class="footer mt-auto">
    <div class="container-fluid d-flex flex-row-reverse">
        <p class="text-secondary">Powered by Tigase XMPP Server ${tigase.server.XMPPServer.getImplementationVersion()}</p>
    </div>
</footer>
<script src="assets/js/bootstrap.bundle.min.js"></script>
<script src="assets/js/prism.js"></script>
</body>
</html>