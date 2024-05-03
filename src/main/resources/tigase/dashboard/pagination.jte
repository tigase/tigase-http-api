@import jakarta.ws.rs.core.UriBuilder
@import jakarta.ws.rs.core.UriInfo
@import tigase.http.jaxrs.Page
@param UriInfo uriInfo
@param Page<?> page
<div class="pagination d-flex justify-content-center">
    !{UriBuilder builder = uriInfo.getRequestUriBuilder();}
    <ul class="pagination pagination-sm rounded">
        <li class="page-item ${page.isFirst() ? " disabled" : ""}">
            <a class="page-link" href="${builder.clone().replaceQueryParam("page", page.previousPageable().pageNumber()).replaceQueryParam("size", page.pageable().pageSize()).build().toString()}">Previous</a>
        </li>
        @for(int i : page.paginate(9))
            <li class="page-item ${((i-1) == page.pageable().pageNumber()) ? " active" : ""}">
                <a class="page-link" href="${builder.clone().replaceQueryParam("page", i-1).replaceQueryParam("size", page.pageable().pageSize()).build().toString()}">${i}</a>
            </li>
        @endfor
        <li class="page-item ${page.isLast() ? " disabled" : ""}">
            <a class="page-link" href="${builder.clone().replaceQueryParam("page", page.nextPageable().pageNumber()).replaceQueryParam("size", page.pageable().pageSize()).build().toString()}">Next</a>
        </li>
    </ul>
</div>