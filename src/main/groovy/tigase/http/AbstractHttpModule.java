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
package tigase.http;

import tigase.http.api.HttpServerIfc;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static tigase.http.modules.Module.HTTP_CONTEXT_PATH_KEY;
import static tigase.http.modules.Module.VHOSTS_KEY;

/**
 * Created by andrzej on 08.08.2016.
 */
public abstract class AbstractHttpModule implements UnregisterAware, Initializable, RegistrarBean, ConfigurationChangedAware {

	private static final Map<String, Kernel> kernels = new ConcurrentHashMap<>();

	public static final Kernel getKernel(String id) {
	 	return kernels.get(id);
	}

	@Inject
	protected HttpServerIfc httpServer;

	@ConfigField(desc = "Context path", alias = HTTP_CONTEXT_PATH_KEY)
	protected String contextPath = null;
	@ConfigField(desc = "List of vhosts", alias = VHOSTS_KEY)
	protected String[] vhosts = null;

	protected final String uuid = UUID.randomUUID().toString();

	public abstract void start();

	public abstract void stop();

	@Override
	public void register(Kernel kernel) {
		kernels.put(uuid, kernel);
	}

	@Override
	public void unregister(Kernel kernel) {
		kernels.remove(uuid);
	}

	@Override
	public void initialize() {
		start();
	}

	@Override
	public void beforeUnregister() {
		stop();
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		if (httpServer == null)
			return;

		start();
	}

}
