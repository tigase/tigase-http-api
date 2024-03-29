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
package tigase.http.jaxrs;

import tigase.http.AuthProvider;
import tigase.http.modules.AbstractBareModule;
import tigase.kernel.beans.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class AbstractJaxRsModule<H extends Handler>
		extends AbstractBareModule
		implements JaxRsModule<H> {

	@Inject(nullAllowed = true)
	private AuthProvider authProvider;

	private ScheduledExecutorService executorService;
	@Inject(nullAllowed = true)
	private List<H> handlers = new ArrayList<>();

	@Override
	public AuthProvider getAuthProvider() {
		return authProvider;
	}

	@Override
	public ScheduledExecutorService getExecutorService() {
		return executorService;
	}

	@Override
	public List<H> getHandlers() {
		return handlers;
	}

	@Override
	public void start() {
		if (executorService != null) {
			executorService.shutdown();
		}
		executorService = Executors.newSingleThreadScheduledExecutor();

		super.start();


	}

	@Override
	public void stop() {
		if (executorService != null) {
			executorService.shutdown();
		}
		super.stop();
	}

}
