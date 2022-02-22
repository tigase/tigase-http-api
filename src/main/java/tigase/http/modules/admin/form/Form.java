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
import java.util.List;
import java.util.Optional;

public class Form {

	private final String title;
	private final String instructions;
	private final List<Field> fields = new ArrayList<>();
	private final List<Reported> reported = new ArrayList<>();

	public Form(List<Element> items) {
		this.title = items.stream().filter(el ->  el.getName() == "title").findFirst().map(Element::getCData).orElse(null);
		this.instructions = items.stream().filter(el ->  el.getName() == "instructions").findFirst().map(Element::getCData).orElse(null);

		Optional<Reported> reported = Optional.empty();
		for (Element el : items) {
			switch (el.getName()) {
				case "reported":
					reported = Optional.of(new Reported(el));
					reported.ifPresent(this.reported::add);
					break;
				case "item":
					reported.ifPresent(r -> r.getItems().add(new Reported.Item(el)));
					break;
				case "field":
					reported = Optional.empty();
					fields.add(new Field(el));
					break;
				default:
					reported = Optional.empty();
					break;
			}
		}
	}

	public String getTitle() {
		return title;
	}

	public String getInstructions() {
		return instructions;
	}

	public List<Reported> getReported() {
		return reported;
	}

	public List<Field> getFields() {
		return fields;
	}

}
