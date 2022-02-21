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

import tigase.xml.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Reported {

	private final String label;
	private List<FieldDefinition> fields;
	private List<Item> items = new ArrayList<>();

	public Reported(Element reportedEl) {
		label = reportedEl.getAttributeStaticStr("label");
		fields = Optional.ofNullable(reportedEl.getChildren())
				.map(fields -> fields.stream().map(FieldDefinition::new).collect(Collectors.toList()))
				.orElse(Collections.emptyList());
	}

	public String getLabel() {
		return label;
	}

	public List<FieldDefinition> getFields() {
		return fields;
	}

	public List<Item> getItems() {
		return items;
	}

	public static class FieldDefinition {

		private final Element field;

		public FieldDefinition(Element field) {
			this.field = field;
		}

		public String getLabel() {
			return Optional.ofNullable(field.getAttributeStaticStr("label"))
					.orElseGet(() -> field.getAttributeStaticStr("var"));
		}

		public String getVar() {
			return field.getAttributeStaticStr("var");
		}

		public String getStyle() {
			return Optional.ofNullable(field.getAttributeStaticStr("align"))
					.map(align -> "text-align: " + align)
					.orElse(null);
		}

	}

	public static class Item {

		private List<Element> fields;

		public Item(Element el) {
			this.fields = Optional.ofNullable(el.getChildren()).orElse(Collections.emptyList());
		}

		public Item.Field getField(String var) {
			return fields.stream()
					.filter(el -> var.equals(el.getAttributeStaticStr("var")))
					.findFirst()
					.map(Item.Field::new)
					.orElse(null);
		}

		public List<Element> getFields() {
			return fields;
		}

		public static class Field {

			private final Element field;

			public Field(Element field) {
				this.field = field;
			}

			public String getValueLabel() {
				return Optional.ofNullable(field.getChildAttributeStaticStr("value", "label"))
						.orElseGet(() -> field.getChildCDataStaticStr(new String[]{"field", "value"}));
			}

		}
	}
}
