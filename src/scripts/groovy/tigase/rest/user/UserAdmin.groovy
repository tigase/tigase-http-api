package tigase.rest.user
/*
 * Tigase HTTP API
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
import tigase.http.rest.Service
import tigase.xmpp.BareJID

/**
 * Class implements ability to manage users for service administrator
 * Handles requests for /rest/user/user@domain where user@domain is jid
 *
 * Example format of content of request or response:
 * <user><jid>user@domain</jid><password>Paa$$w0rd</password></jid></user>
 */
class UserAdminHandler extends tigase.http.rest.Handler {

    public UserAdminHandler() {
        regex = /\/([^@\/]+)@([^@\/]+)/
		authRequired = { api_key -> return api_key == null && requiredRole != null }
        requiredRole = "admin"
        isAsync = false
        execGet = { Service service, callback, user, localPart, domain ->
            def jid = BareJID.bareJIDInstance(localPart, domain);
            def uid = service.getUserRepository().getUserUID(jid);
            if (uid <= 0) {
                callback(null);
            }
            else {
                callback([user:[jid:"$localPart@$domain", domain:domain, uid:uid]]);
            }
        }
        execPut = { Service service, callback, user, content, localPart, domain ->
            def jid = BareJID.bareJIDInstance(localPart, domain);
            def password = content.user.password;
			def email = content.user.email;
            service.getAuthRepository().addUser(jid, password);
            def uid = service.getUserRepository().getUserUID(jid);
			if (uid && email) {
				service.getUserRepository().setData(jid, "email", email);
			}
            callback([user:[jid:"$localPart@$domain", domain:domain, uid: uid]]);
        }
        execDelete = { Service service, callback, user, localPart, domain ->
            def jid = BareJID.bareJIDInstance(localPart, domain);
            service.getAuthRepository().removeUser(jid)
            callback([user:[jid:"$localPart@$domain", domain:domain]]);
        }
        execPost = { Service service, callback, user, content, localPart, domain ->
            def jid = BareJID.bareJIDInstance(localPart, domain);
            def password = content.user.password;
            service.getAuthRepository().updatePassword(jid, password)
            def uid = service.getUserRepository().getUserUID(jid);
            callback([user:[jid:"$localPart@$domain", domain:domain, uid: uid]]);
        }
    }

}