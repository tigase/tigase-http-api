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
package tigase.http.api.marshallers;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TestObject {

	@XmlElement
	private String title;

	@XmlAttribute
	private String id;

	@XmlAttribute
	private LocalDate data = LocalDate.now();
	private Date date = new Date();

	@XmlElement
	private List<TestObject> items = new ArrayList<>();

	public TestObject() {
	}

	public TestObject(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}

	public List<TestObject> getItems() {
		return items;
	}

	public void setItems(List<TestObject> items) {
		this.items = items;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TestObject)) {
			return false;
		}
		TestObject data1 = (TestObject) o;
		return Objects.equals(title, data1.title) && Objects.equals(id, data1.id) &&
				Objects.equals(data, data1.data) && Objects.equals(date, data1.date) &&
				Objects.equals(items, data1.items);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, id, data, date, items);
	}

	@Override
	public String toString() {
		return "TestObject{" + "title='" + title + '\'' + ", id='" + id + '\'' + ", data=" + data + ", date=" + date +
				", items=" + items + '}';
	}
}

