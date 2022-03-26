@import tigase.http.modules.rest.handlers.UserResetPasswordHandler
@import tigase.xmpp.jid.BareJID;
@import java.util.List
@param BareJID jid
@param String email
@param List<String> errors
@param String basePath
@param UserResetPasswordHandler.Captcha captcha
@template.header(basePath=basePath)
<main class="container" role="main">
    <div class="row">
        @if(errors.isEmpty() && jid != null)
            <div class="card bg-success">
                <div class="panel-heading panel-xs">
                    <h4>Check your email</h4>
                </div>
                <div class="card-body">
                    <h5 class="card-titlet">Check your email</h5>
                    <h5>Email was sent to <strong>${email}</strong>. Click the link in the email to reset password for account <strong>${jid.toString()}</strong>.</h5>
                    <h5>If you don't see the email, check other mail folders, like junk and spam.</h5>
                </div>
            </div>
        @else
            <form method="POST" class="form-horizontal">
                <div class="card">
                    <div class="panel-body">
                        <h5 class="card-title">Reset account password</h5>
                        <p class="card-text">Please fill out this form to reset password for an account.</p>
                        <div class="row">
                            <label class="col-sm-2 col-form-label" for='jid'>JID:</label>
                            <div class="col-sm-10">
                                <input class="form-control" type='text' size='90' name='jid' value="${jid != null ? jid.toString() : ""}" required/>
                                <small class="text-muted">XMPP account address</small>
                            </div>
                        </div>
                        <div class="row">
                            <label class="col-sm-2 col-form-label" for='email'>Email:</label>
                            <div class="col-sm-10">
                                <input class="form-control" type='text' size='90' name='email' value="${email}" required/>
                                <small class="text-muted">Email used during registration</small>
                            </div>
                        </div>
                        @if(captcha != null)
                        <input name="id" type="hidden" value="${captcha.getId()}" />
                        <input name="captcha-query" type="hidden" value="${captcha.getQuery()}" />
                        <div class="row">
                            <label class="col-sm-2 col-form-label" for='captcha'>${captcha.getQuery()} = </label>
                            <div class="col-sm-10">
                                <input class="form-control" type='text' size='90' name='captcha' value="" required/>
                            </div>
                        </div>
                        @endif
                    </div>
                    @if(!errors.isEmpty())
                    <div class="alert alert-danger" role="alert" style="margin: 0px">
                        <h4>Error</h4>
                        @for(String error : errors)
                        <h5>${error}</h5><% } %>
                        @endfor
                    </div>
                    @endif
                    <% } %>
                </div>
                <div class="m-3">
                    <button class="btn btn-primary" type="submit">Reset password</button>
                </div>
            </form>
        @endif
    </div>
</main>
@template.footer()