@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.DashboardHandler;
@param UriInfo uriInfo

<link rel="stylesheet" type="text/css" href="${uriInfo.getBaseUriBuilder().path("assets/css/bootstrap.min.css").build().getPath()}">
<link rel="stylesheet" type="text/css" href="${uriInfo.getBaseUriBuilder().path("assets/css/custom.css").build().getPath()}">
<link rel="stylesheet" type="text/css" href="${uriInfo.getBaseUriBuilder().path("assets/css/http-api-custom.css").build().getPath()}">
<link rel="stylesheet" type="text/css" href="${uriInfo.getBaseUriBuilder().path("assets/css/bootstrap-icons.min.css").build().getPath()}">
<%--<script src="${uriInfo.getBaseUriBuilder().path("assets/js/bootstrap.bundle.min.js").build().getPath()}"></script>--%>
<link rel="stylesheet" type="text/css" href="${uriInfo.getBaseUriBuilder().path("assets/css/cropper.min.css").build().getPath()}">
<script src="${uriInfo.getBaseUriBuilder().path("assets/js/cropper.min.js").build().getPath()}"></script>
!{var customAssets = DashboardHandler.getHandler().getCustomAssets();}
@for(var cssfile : customAssets.cssFiles())
    <link rel="stylesheet" type="text/css" href="${uriInfo.getBaseUriBuilder().path("assets/" + cssfile).build().getPath()}">
@endfor
@for(var jsFile : customAssets.jsFiles())
    <script src="${uriInfo.getBaseUriBuilder().path("assets/" + jsFile).build().getPath()}"></script>
@endfor