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
package tigase.http.modules.admin.form;

import tigase.util.datetime.TimestampHelper;
import tigase.xml.Element;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class Field {

	private static final TimestampHelper timestampHelper = new TimestampHelper();
	
	private final Element field;

	public Field(Element field) {
		this.field = field;
	}

	public String getType() {
		return Optional.ofNullable(field.getAttributeStaticStr("type")).orElse("text-single");
	}

	public String getSubType() {
		return field.getAttributeStaticStr("subtype");
	}

	public String getVar() {
		return field.getAttributeStaticStr("var");
	}

	public String getValue() {
		return field.getChildCData(new String[]{"field", "value"});
	}

	public LocalDateTime getDateTimeValue() {
		return Optional.ofNullable(field.getChildCDataStaticStr(new String[]{"field", "value"}))
				.map(this::parseTimestamp)
				.map(value -> LocalDateTime.ofInstant(value.toInstant(), java.time.ZoneId.systemDefault()))
				.orElse(null);
	}

	public LocalDate getDate() {
		return Optional.ofNullable(getDateTimeValue()).map(LocalDateTime::toLocalDate).orElse(null);
	}

	public String getDateString() {
		return Optional.ofNullable(getDate()).map(LocalDate::toString).orElse(null);
	}

	public LocalTime getTime() {
		return Optional.ofNullable(getDateTimeValue()).map(LocalDateTime::toLocalTime).orElse(null);
	}

	public String getTimeString() {
		return Optional.ofNullable(getTime()).map(LocalTime::toString).orElse(null);
	}

	public TimeZone getTimeZone() {
		return TimeZone.getDefault();
	}

	private Date parseTimestamp(String text) {
		try {
			return timestampHelper.parseTimestamp(text);
		} catch (ParseException ex) {
			// ignoring...
			return null;
		}
	}

	public String getMultilineTextValue() {
		return Optional.ofNullable(field.getChildren(el -> el.getName() == "value"))
				.orElse(Collections.emptyList())
				.stream()
				.map(Element::getCData)
				.collect(Collectors.joining("\n"));
	}

	public String getLabel() {
		return Optional.ofNullable(field.getAttributeStaticStr("label"))
				.orElseGet(() -> field.getAttributeStaticStr("var"));
	}

	public boolean isChecked() {
		return Optional.ofNullable(getValue()).map(value -> "true".equals(value) || "1".equals(value)).orElse(false);
	}

	public boolean isRequired() {
		return field.getChild("required") != null;
	}

	public String getDesc() {
		return field.getChildCData(new String[]{"field", "desc"});
	}

	public boolean isMultiple() {
		return getType().contains("-multi");
	}

	public List<Option> getOptions() {
		List<Element> children = field.getChildren();
		if (children == null) {
			return Collections.emptyList();
		}
		return children.stream().filter(el -> el.getName() == "option").map(Option::new).collect(Collectors.toList());
	}

	public List<String> getValues() {
		return Optional.ofNullable(field.getChildren(el -> el.getName() == "value"))
				.orElse(Collections.emptyList())
				.stream()
				.map(Element::getCData)
				.collect(Collectors.toList());
	}

	public boolean isSelected(Option option) {
		return Optional.ofNullable(getValue()).map(value -> Objects.equals(option.getValue(), value)).orElse(false);
	}

	public static class Option {

		private final Element option;

		public Option(Element option) {
			this.option = option;
		}

		public String getLabel() {
			return Optional.ofNullable(option.getAttributeStaticStr("label")).orElseGet(() -> getValue());
		}

		public String getValue() {
			return Optional.ofNullable(option.findChild(el -> el.getName() == "value"))
					.map(Element::getCData)
					.orElse(null);
		}
	}
}
