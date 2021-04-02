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
package tigase.http.upload.commands;

import tigase.http.upload.FileUploadComponent;
import tigase.kernel.beans.Bean;

@Bean(name = "delete-slot-command-admin", parent = FileUploadComponent.class, active = true)
public class DeleteFileCommandAdmin extends DeleteFileCommandAbstract {

	public DeleteFileCommandAdmin() {
		super(true);
	}

	@Override
	public String getName() {
		return "Delete slot (admin)";
	}

	@Override
	public String getNode() {
		return "delete-slot-admin";
	}

}
