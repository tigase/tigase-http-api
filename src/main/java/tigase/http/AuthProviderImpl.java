package tigase.http;

import tigase.auth.credentials.Credentials;
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.Optional;

@Bean(name = "authProvider", parent = HttpMessageReceiver.class, active = true, exportable = true)
public class AuthProviderImpl
		implements AuthProvider {

	@Inject(nullAllowed = true)
	private UserRepository userRepository;
	@Inject(nullAllowed = true)
	private AuthRepository authRepository;
	@Inject(bean = "service")
	private HttpMessageReceiver receiver;

	public AuthProviderImpl() {
	}

	@Override
	public boolean isAdmin(BareJID user) {
		return receiver.isAdmin(JID.jidInstance(user));
	}

	@Override
	public boolean checkCredentials(String user, final String password)
			throws TigaseStringprepException, TigaseDBException, AuthorizationException {
		if (authRepository == null) {
			return false;
		}
		BareJID jid = BareJID.bareJIDInstance(user);
		Credentials credentials = authRepository.getCredentials(jid, Credentials.DEFAULT_CREDENTIAL_ID);
		if (credentials == null) {
			return false;
		}

		return Optional.ofNullable(credentials.getFirst()).map(e -> e.verifyPlainPassword(password)).orElse(false);
	}
}
