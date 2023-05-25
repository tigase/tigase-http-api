/*
 * Tigase HTTP API component - Tigase HTTP API component
 * Copyright (C) 2013 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.rest.user

import tigase.db.UserNotFoundException
import tigase.http.rest.Service
import tigase.kernel.beans.Bean
import tigase.kernel.beans.Inject
import tigase.vhosts.VHostItem
import tigase.vhosts.VHostManager
import tigase.xmpp.jid.BareJID

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Class implements ability to manage users for service administrator
 * Handles requests for /rest/user/user@domain where user@domain is jid
 *
 * Example format of content of request or response:
 * <user><jid>user@domain</jid><password>Paa$$w0rd</password></jid></user>*/
@Bean(name = "user-admin-handler", active = true)
class UserAdminHandler
		extends tigase.http.rest.Handler {

	def log = Logger.getLogger("tigase.rest")

	def TIMEOUT = 30 * 1000;
	def COMMAND_XMLNS = "http://jabber.org/protocol/commands";

	@Inject
	private VHostManager vHostManager;

	public UserAdminHandler() {
		description = [ regex : "/{user_jid}",
						GET   : [ info       : 'Retrieve user account details',
								  description: """Only required parameter is part of url {user_jid} which is jid of user which account informations you want to retrieve.
Data will be returned in form of JSON or XML depending on selected format by Accept HTTP header\n\

Example response:
\${util.formatData([user:[jid:'user@example.com', domain:'example.com', uid:10 ]])}				
""" ],
						PUT   : [ info       : 'Create new user account',
								  description: """Part of url {user_jid} is parameter which is jid of user which account you want to create, ie. user@example.com.
To create account additional data needs to be passed as content of HTTP request:
\${util.formatData([user:[password:'some_password',email:'user@example.com']])}

Data will be returned in form of JSON or XML depending on selected format by Accept HTTP header

Example response:
\${util.formatData([user:[jid:'user@example.com', domain:'example.com', uid:10 ]])}				
""" ],
						POST  : [ info       : 'Update user account',
								  description: """Part of url {user_jid} is parameter which is jid of user which account you want to update, ie. user@example.com.\n\
Additional data needs to be passed as content of HTTP request to change password for this account:
\${util.formatData([user:[password:'some_password']])}
Data will be returned in form of JSON or XML depending on selected format by Accept HTTP header

Example response:
\${util.formatData([user:[jid:'user@example.com', domain:'example.com', uid:10 ]])}				
""" ],
						DELETE: [ info       : 'Delete user account',
								  description: """Part of url {user_jid} is parameter which is jid of user which account you want to remove, ie. user@example.com.
Data will be returned in form of JSON or XML depending on selected format by Accept HTTP header

Example response:
\${util.formatData([user:[jid:'user@example.com', domain:'example.com', uid:10 ]])}				
""" ], ];
		regex = /\/([^@\/]+)@([^@\/]+)/
		authRequired = { api_key -> return api_key == null && requiredRole != null }
		requiredRole = "admin"
		isAsync = true
		execGet = { Service service, callback, user, localPart, domain ->
			def jid = BareJID.bareJIDInstance(localPart, domain);
			def uid = service.getUserRepository().getUserUID(jid);
			if (!service.getUserRepository().userExists(jid)) {
				callback(null);
			} else {
				def isAdmin = vHostManager.getVHostItem(jid.getDomain()).isAdmin(jid.toString())
				callback([ user: [ jid: jid.toString(), domain: domain, isAdmin: isAdmin, uid: uid ] ]);
			}
		}
		execPut = { Service service, callback, user, content, localPart, domain ->
			def jid = BareJID.bareJIDInstance(localPart, domain);
			def password = content.user.password;
			def email = content.user.email;
			try {
				service.getAuthRepository().addUser(jid, password);
				def uid = service.getUserRepository().getUserUID(jid);
				if (service.getUserRepository().userExists(jid) && email) {
					service.getUserRepository().setData(jid, "email", email);
				}
				def setAsAdmin = content.user.isAdmin;
				if (setAsAdmin != null) {
					updateIsAdmin(jid.domain, jid, setAsAdmin);
				}
				def isAdmin = vHostManager.getVHostItem(jid.getDomain()).isAdmin(jid.toString())
				callback([ user: [ jid: jid.toString(), domain: domain, isAdmin: isAdmin, uid: uid ] ]);
			} catch (tigase.db.UserExistsException ex) {
				callback({ req, resp -> resp.sendError(409, "User exists");
				});
			}
		}
		execDelete = { Service service, callback, user, localPart, domain ->
			def jid = BareJID.bareJIDInstance(localPart, domain);
			def uid = service.getUserRepository().getUserUID(jid);
			def exists = service.getUserRepository().userExists(jid)
			log.log(Level.FINEST, "Call to remove user: ${jid}, uid: ${uid}, exists: ${exists}")
			if (!exists) {
				callback(null);
			} else {
				service.getAuthRepository().removeUser(jid);
				try {
					service.getUserRepository().removeUser(jid)
				} catch (UserNotFoundException ex) {
					// We ignore this error here. If auth_repo and user_repo are in fact the same
					// database, then user has been already removed with the auth_repo.removeUser(...)
					// then the second call to user_repo may throw the exception which is fine.
				}
				callback([ user: [ jid: jid.toString(), domain: domain, uid: uid ] ]);
			}
		}
		execPost = { Service service, callback, user, content, localPart, domain ->
			def jid = BareJID.bareJIDInstance(localPart, domain);
			def password = content.user.password;
			if (password != null) {
				service.getAuthRepository().updatePassword(jid, password)
			}
			def setAsAdmin = content.user.isAdmin;
			if (setAsAdmin != null) {
				 updateIsAdmin(jid.domain, jid, setAsAdmin);
			}
			def uid = service.getUserRepository().getUserUID(jid);
			def isAdmin = vHostManager.getVHostItem(jid.getDomain()).isAdmin(jid.toString())
			callback([ user: [ jid: jid.toString(), domain: domain, isAdmin: isAdmin, uid: uid ] ]);
		}
	}

	void updateIsAdmin(String domain, BareJID jid, Object setAsAdmin) {
		setAsAdmin = Boolean.parseBoolean("$setAsAdmin");
		VHostItem oldVhost = vHostManager.componentRepository.getItem(jid.getDomain());
		if (oldVhost != null) {
			if (oldVhost.isAdmin(jid.toString()) != setAsAdmin) {
				def vhost = vHostManager.componentRepository.getItemInstance();
				vhost.initFromElement(oldVhost.toElement());
				def admins = (vhost.getAdmins() as List<String>) ?: [];
				admins.remove(jid.toString());
				if (setAsAdmin) {
					admins.add(jid.toString());
				}
				vhost.item.setAdmins(admins as String[]);
				vhost.refresh();
				vHostManager.componentRepository.addItem(vhost);
			}
		}
	}

}