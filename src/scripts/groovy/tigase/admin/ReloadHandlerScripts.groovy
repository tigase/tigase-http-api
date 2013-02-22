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

/*
 Reload REST handlers

 AS:Description: Reload REST handlers
 AS:CommandId: rest-handlers-reload
 AS:Component: rest
 */

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.vhosts.*


def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def result = p.commandResult(Command.DataType.result)

try {
    if (!isServiceAdmin) {
        Command.addTextField(result, "Error", "You do not have enough permissions to create account for this domain.");
    }
    else {
        restMessageReceiver.reloadRestHandlers();
        Command.addTextField(result, "Note", "Operation successful");
    }
}
catch (Exception ex) {
    Command.addTextField(result, "Note", "Problem with reloading REST handlers.");
}

return result
