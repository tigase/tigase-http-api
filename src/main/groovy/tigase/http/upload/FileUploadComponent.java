/*
 * Tigase HTTP API
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.upload;

import tigase.component.AbstractKernelBasedComponent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;

/**
 * Created by andrzej on 06.08.2016.
 */
@Bean(name = "upload", parent = Kernel.class, active = false)
public class FileUploadComponent extends AbstractKernelBasedComponent {

	@Inject
	private HttpModule httpModule;

	@Override
	public String getComponentVersion() {
		String version = this.getClass().getPackage().getImplementationVersion();
		return version == null ? "0.0.0" : version;
	}

	@Override
	public String getDiscoCategory() {
		return "store";
	}

	@Override
	public String getDiscoDescription() {
		return "HTTP File Upload component";
	}

	@Override
	public String getDiscoCategoryType() {
		return "file";
	}

	@Override
	public boolean isSubdomain() {
		return true;
	}

	@Override
	public boolean isDiscoNonAdmin() {
		return true;
	}

	@Override
	protected void registerModules(Kernel kernel) {
	}

}
