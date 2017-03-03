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

import tigase.component.ScheduledTask;
import tigase.http.upload.logic.Logic;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;

import java.time.Duration;

/**
 * Created by andrzej on 09.08.2016.
 */
@Bean(name = "expiration", parent = FileUploadComponent.class, active = true)
public class ExpirationTask extends ScheduledTask {

	@ConfigField(desc = "Expiration time", alias = "expiration-time")
	private Duration expirationTime = Duration.ofDays(30);

	@ConfigField(desc = "Limit of slots cleared at once")
	private int limit = 10000;

	@Inject
	private Logic logic;

	public ExpirationTask() {
		super(Duration.ZERO, Duration.ofDays(1));
	}

	@Override
	public void run() {
		logic.removeExpired(expirationTime, limit);
	}
}
