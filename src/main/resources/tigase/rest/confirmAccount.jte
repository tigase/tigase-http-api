@import tigase.xmpp.jid.BareJID
@param BareJID account
@param Throwable exception
@param String basePath
@template.header(basePath=basePath)
<main class="container" role="main">
    <div class="row">
        @if(account != null)
            <div class="card panel-default bg-success mb-4">
                <div class="card-body">
                    <h5 class="card-title">Account confirmation successful.</h5>
                    <p class="card-text">Your new account ${account.toString()} is now active!</p>
                </div>
            </div>
        @else
            <div class="card panel-default bg-danger mb-4">
                <div class="card-body">
                    <h5 class="card-title">Account confirmation failure.</h5>
                    <p class="card-text">@if(exception != null) ${exception.getMessage()} @else Unknown error occurred. @endif</p>
                </div>
            </div>
        @endif
    </div>
</main>
@template.footer()