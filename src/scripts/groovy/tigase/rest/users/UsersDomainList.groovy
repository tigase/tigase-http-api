package tigase.rest.users
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

class UsersHandler extends tigase.http.rest.Handler {

    public UsersHandler() {
        regex = /\/([^@\/]+)/
        requiredRole = "admin"
        isAsync = false
        execGet = { Service service, callback, jid, domain ->
            def repo = service.getUserRepository().getRepo(domain);
            if (!repo) callback(null);
            def users = repo.getUsers().findAll { it.getDomain() == domain };
            callback([users:[items:users, count:users.size()]]);
        }
    }

}