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
package tigase.rest.vhost

import tigase.http.rest.Service
import tigase.kernel.beans.Bean
import tigase.kernel.beans.Inject
import tigase.vhosts.VHostManager

import java.util.logging.Logger

@Bean(name = "vhost-admin-bean", active = true)
class VhostAdminHandler
		extends tigase.http.rest.Handler {

	def log = Logger.getLogger("tigase.rest")

	@Inject
	private VHostManager vHostManager;

	public VhostAdminHandler() {
		description = [ regex : "/",
						GET   : [ info       : 'Retrieve list of VHosts',
								  description: """No parameters is required, Only returns list of the domains.
Data will be returned in form of JSON or XML depending on selected format by Accept HTTP header\n\

Example response:
\${util.formatData([vhosts:['example.com', 'example2.com']])}				
""" ],];
		regex = /\//
		authRequired = { api_key -> return api_key == null && requiredRole != null }
		requiredRole = "admin"
		isAsync = true
		execGet = { Service service, callback, user ->
			var mainVhost = vHostManager.getDefVHostItem();
			callback([ vhosts: vHostManager.getAllVHosts()
					.findAll { it -> "default" != it.toString()}
					.collect {it -> {
						var item = [vhost: it.toString()]
						if (it.getBareJID() == mainVhost) {
							item."default-virtual-host" = true;
						}
						return item;
			} } ]);
		}
	}

}