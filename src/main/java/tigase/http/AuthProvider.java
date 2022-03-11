package tigase.http;

import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

public interface AuthProvider {

	boolean isAdmin(BareJID user);

	boolean checkCredentials(String user, String password)
			throws TigaseStringprepException, TigaseDBException, AuthorizationException;
}
