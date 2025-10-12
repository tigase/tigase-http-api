@import jakarta.ws.rs.core.SecurityContext
@import jakarta.ws.rs.core.UriInfo
@import tigase.http.modules.dashboard.ServerInfoHandler
@import javax.servlet.http.HttpServletRequest
@param UriInfo uriInfo
@param SecurityContext securityContext
@param HttpServletRequest request
@param String dataJson

@template.layout(securityContext = securityContext, uriInfo = uriInfo, request = request, content = @`
    <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.7.1/css/all.css"
          integrity="sha384-fnmOCqbTlWIlj8LyTjo7mOUStjsKC4pOpQbqyi7RrhN7udi9RwhKkMHpvLbHG9Sr" crossorigin="anonymous">
    <style>
        .infocards div.card > .card-body span.h2 {
            font-size: 20px;
            font-family: Nunito, -apple-system, system-ui, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
            color: #5a5c69;
        }

        .infocards div.card {
            position: relative;
            display: flex;
            flex-direction: column;
            min-width: 0;
            word-wrap: break-word;
            background-color: #fff;
            background-clip: border-box;
            border: 1px solid #edf2f9;
            border-radius: .5rem;
        }

        .infocards .card-title {
            color: #95aac9;
            font-family: "Cerebri Sans", sans-serif;
            font-size: 10px;
            font-weight: 500;
            letter-spacing: 0.8px;


        }
    </style>
    <script>
        var json = $unsafe{dataJson};
    </script>
    <div class="container mt-3 infocards">
        <div class="row">
            <div class="col-sm-4 mb-4">
                <div class="card shadow">
                    <div class="card-body">
                        <div class="row no-gutters align-items-center">
                            <div class="col">
                                <h6 class="card-title text-uppercase text-muted mb-2">
                                    CPU usage
                                </h6>
                                <span class="h2 mb-0"><span class="data-cpu-usage-proc"></span>%</span>
                                <div class="col">
                                    <div class="progress mr-2 ml-2" style="height: 5px;">
                                        <div id="data-cpu-usage-proc" class="progress-bar bg-info"
                                             role="progressbar"
                                             style="width: 0%"></div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-auto ">
                                <i class="fas fa-microchip fa-2x" style="color: #dddfeb;"></i></div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-sm-4 mb-4">
                <div class="card shadow">
                    <div class="card-body">
                        <div class="row no-gutters align-items-center">
                            <div class="col">
                                <h6 class="card-title text-uppercase text-muted mb-2">
                                    HEAP usage
                                </h6>
                                <span class="h2 mb-0"><span class="data-heap-usage-proc"></span>%</span>
                                <div class="col">
                                    <div class="progress mr-2 ml-2" style="height: 5px;">
                                        <div id="data-heap-usage-proc" class="progress-bar bg-info"
                                             role="progressbar"
                                             style="width: 0%"></div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-auto ">
                                <i class="fas fa-memory fa-2x" style="color: #dddfeb;"></i></div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-sm-4 mb-4">
                <div class="card shadow">
                    <div class="card-body">
                        <div class="row no-gutters align-items-center">
                        <div class="col">
                            <h6 class="card-title text-uppercase text-muted mb-2">
                                Load average
                            </h6>
                            <span id="data-load-average" class="h2 mb-0">&nbsp;</span>
                            <div class="col" style="height: 5px;">
                            </div>
                        </div>
                        <div class="col-auto ">
                            <i class="fas fa-tachometer-alt fa-2x" style="color: #dddfeb;"></i></div>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </div>

    <div class="container mb-3">
        <div class="card">
            <div class="card-body">
                <div class="card-title row">
                    <h5 class="col">Details</h5>
                </div>
                <table class="table">
                    <tbody>
                    <tr>
                        <th scope="row" style="width: 20%">Server version</th>
                        <td class="version"></td>
                    </tr>
                    <tr>
                        <th scope="row" style="width: 20%">Uptime</th>
                        <td class="data-uptime"></td>
                    </tr>
                    <tr>
                        <th scope="row">Load average</th>
                        <td><span class="data-load-average"></span></td>
                    </tr>
                    <tr>
                        <th scope="row">CPUs no</th>
                        <td class="data-cpus-no"></td>
                    </tr>
                    <tr>
                        <th scope="row">Threads</th>
                        <td class="data-threads-count"></td>
                    </tr>
                    <tr>
                        <th scope="row">CPU usage</th>
                        <td><span class="data-cpu-usage-proc"></span>%</td>
                    </tr>
                    <tr>
                        <th scope="row">HEAP usage</th>
                        <td><span class="data-heap-usage-proc"></span>%</td>
                    </tr>
                    <tr>
                        <th scope="row">NONHEAP usage</th>
                        <td><span class="data-nonheap-usage-proc"></span>%</td>
                    </tr>

                    </tbody>
                </table>
            </div>
        </div>


        <table>

        </table>

    </div>
    <script language="JavaScript">
        function renderData() {
            for (key in json) {
                if (key.endsWith("-proc")) {
                    let el = document.getElementById(key);
                    if (el) {
                        el.style.width = json[key] + "%"
                    }
                    let elems = document.getElementsByClassName(key);
                    for (let i = 0; i < elems.length; i++) {
                        elems[i].textContent = json[key];
                    }
                } else {
                    let el = document.getElementById(key);
                    if (el) {
                        el.textContent = json[key]
                    }
                    let elems = document.getElementsByClassName(key);
                    for (let i = 0; i < elems.length; i++) {
                        elems[i].textContent = json[key];
                    }
                }
            }
        }
        function updateData() {
            let xhttp = new XMLHttpRequest();
            xhttp.open("GET", "${uriInfo.getBaseUriBuilder().path(ServerInfoHandler.class, "infoJson").build().getPath()}")
            xhttp.setRequestHeader("Accept", "application/json")
            xhttp.onreadystatechange = function () {
                if (xhttp.readyState === 4 && xhttp.status === 200) {
                    json =
                        JSON.parse(xhttp.responseText);
                    renderData();
                    scheduleUpdateData();
                }
            };
            xhttp.send()
        }
        function scheduleUpdateData() {
            window.setTimeout(() => {
                updateData();
            }, 10000);
        }
        window.addEventListener("load", ()=> {
            renderData();
            scheduleUpdateData();
        })
    </script>
`)