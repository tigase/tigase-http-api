@import tigase.xmpp.jid.BareJID
@param BareJID jid
@param String error
@param String basePath
@template.header(basePath=basePath)
<main class="container" role="main">
    <div class="row">
        @if(error != null)
            <div class="card bg-danger mb-4">
                <div class="card-body">
                    <h5 class="card-title">Password reset failure.</h5>
                    <p class="card-text">${error}</p>
                </div>
            </div>
        @else
            <div class="card bg-success mb-4">
                <div class="card-body">
                    <h5 class="card-title">Password changed!</h5>
                    <p class="card-text">Password for account <strong>${jid.toString()}</strong> was successfully changed.</p>
                </div>
            </div>
        @endif
    </div>
</main>
@template.footer()