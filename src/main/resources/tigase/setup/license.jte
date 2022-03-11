@import tigase.http.modules.setup.SetupHandler
@import tigase.http.modules.setup.Config
@import java.util.List
@param Config config
@param List<SetupHandler> pages
@param SetupHandler currentPage
@template.header(pages=pages,currentPage=currentPage)
<form class="form-horizontal" method='POST'>
    <div class="card mt-3">
        <div class="card-body">
@if(config.installationContainsACS())
    <p>This installation package contains a free trial version of the Tigase Advanced Clustering Strategy (&quot;ACS&quot;) software, which you as a licensee may use for the term of your agreement with Tigase.</p>
    <p>ACS is not open source software, it is Tigase&#96;s proprietary software and constitutes the valuable intellectual property of Tigase.</p>
    <p>The free trial granted hereunder does not grant you the right to sublicense ACS to third parties, to copy ACS for third party use, or to use ACS in connection with production systems or for commercial purposes. ACS is provided free of charge for non-commercial testing and development purposes only. Any use of ACS other than for non-commercial testing and development purposes requires the purchase of a license from Tigase.</p>
    <p>If you activate the ACS software under this free trial, you should understand that it will send certain statistical information to Tigase&#96;s servers on a regular basis. If ACS cannot access our servers to send this statistical information, it will stop working. If this occurs, please contact your Tigase representative to resolve the issue and discuss upgrading to a full version of the ACS software. If ACS is installed but not activated, no statistical information will be sent to Tigase&#96;s servers.</p>
    <p>If you enjoy your free trial version of ACS, please contact your Tigase representative to obtain a commercial license for the full version of the ACS software. The full commercial version of ACS does not send statistical information to Tigase's servers.</p>
    <p>By activating this free trial version of ACS you agree and accept that certain statistical information of yours (such as DNS domain names, host names, number of online users, number of cluster nodes, etc.) which may be considered confidential and proprietary, will be sent to Tigase. You accept and confirm that such information, which may be considered confidential or proprietary, may be transferred to Tigase&#96;s servers. You hereby consent to such transfer and waive any rights to this information. You also hereby agree that you will not pursue any remedy at law as a result of the information transfer.</p>
    <div class="mb-3">
        <label class="form-label" for='companyName'>To confirm please enter your name or company name below</label>
        <input class="form-control" type='text' size='40' name='companyName' required value='${config.getCompanyName()}' />
    </div>
@else
<p>This installation package contains an open source version of Tigase XMPP Server, which is available to you under the <a href="license/agpl.html" target="_blank">GNU Affero General Public License</a>.</p>
@endif
        </div>
    </div>
    <div class="m-3">
        <button class="btn btn-secondary" name="submit" type="next">Next</button>
    </div>
</form>
@template.footer()