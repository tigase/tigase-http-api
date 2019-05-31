/**
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
package tigase.http.modules.setup.questions;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MultiAnswerQuestion extends Question {

	private final Supplier<String[]> getter;
	private final Consumer<String[]> setter;

	public MultiAnswerQuestion(String id, Supplier<String[]> getter, Consumer<String[]> setter) {
		super(id);
		this.getter = getter;
		this.setter = setter;
	}

	public String[] getValues() {
		return getter.get();
	}

	public void setValues(String[] value) {
		setter.accept(value);
	}

	@Override
	public boolean isValid() {
		return true;
	}
}
