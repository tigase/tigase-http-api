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
            <form method="POST" class="form-horizontal">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">Reset your password</h5>
                        <p class="card-text">Please enter new password for account <strong>${jid.toString()}</strong></p>
                        <div class="row">
                            <label class="col-sm-3 col-form-label" for='password-entry'>New password:</label>
                            <div class="col-sm-9">
                                <input class="form-control" type='password' size='90' name='password-entry' value="" required/>
                            </div>
                        </div>
                        <div class="row">
                            <label class="col-sm-3 col-form-label" for='password'>Reenter password:</label>
                            <div class="col-sm-9">
                                <input class="form-control" type='password' size='90' name='password-reentry' value="" required/>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="m-3">
                    <button class="btn btn-primary" type="submit">Reset password</button>
                </div>
            </form>
        @endif
    </div>
</main>
@template.footer()