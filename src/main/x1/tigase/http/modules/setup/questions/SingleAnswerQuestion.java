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
package tigase.http.modules.setup.questions;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SingleAnswerQuestion extends Question {

	private final Supplier<String> getter;
	private final String label;
	private final Consumer<String> setter;

	public SingleAnswerQuestion(String id, Supplier<String> getter, Consumer<String> setter) {
		this(id, false, getter, setter);
	}

	public SingleAnswerQuestion(String id, boolean required, Supplier<String> getter, Consumer<String> setter) {
		this(id, null, required, getter, setter);
	}

	public SingleAnswerQuestion(String id, String label, Supplier<String> getter, Consumer<String> setter) {
		this(id, label, false, getter, setter);
	}
	
	public SingleAnswerQuestion(String id, String label, boolean required, Supplier<String> getter, Consumer<String> setter) {
		super(id);
		this.label = label;
		this.getter = getter;
		this.setter = setter;
		setRequired(required);
	}

	public String getLabel() {
		return label;
	}

	public String getValue() {
		return getter.get();
	}

	public void setValue(String value) {
		setter.accept(value);
	}

	public boolean isSelected(String value) {
		return value != null && value.equals(getValue());
	}

	@Override
	public boolean isValid() {
		return !isRequired() || (getValue() != null && !getValue().isEmpty());
	}

	@Override
	public void setValues(String[] values) {
		setValue((values == null || values.length == 0) ? null : (values[0].trim().isEmpty() ? null : values[0]));
	}

}
