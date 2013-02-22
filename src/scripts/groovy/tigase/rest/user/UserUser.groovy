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

class UserHandler extends tigase.http.rest.Handler {

    public UserHandler() {
        regex = /\//
        requiredRole = "user"
        isAsync = false
        execGet = { Service service, callback, jid ->
            def uid = service.getUserRepository().getUserUID(jid);
            if (uid <= 0) {
                callback(null);
            }
            else {
                callback([user:[jid:"${jid.toString()}", domain:jid.getDomain(), uid:uid]]);
            }
        }
        execDelete = { Service service, callback, jid ->
            service.getAuthRepository().removeUser(jid)
            callback([user:[jid:"${jid.toString()}", domain:jid.getDomain(), uid:uid]]);
        }
        execPost = { Service service, callback, content, jid ->
            def password = content.user.password;
            service.getAuthRepository().updatePassword(jid, password)
            def uid = service.getUserRepository().getUserUID(jid);
            callback([user:[jid:"${jid.toString()}", domain:jid.getDomain(), uid:uid]]);
        }
    }

}