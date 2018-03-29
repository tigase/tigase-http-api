/*
 * ApiKeyItem.java
 *
 * Tigase HTTP API
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.modules.rest;

import tigase.db.comp.RepositoryItemAbstract;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class ApiKeyItem
		extends RepositoryItemAbstract {

	private static final String ELEM_NAME = "api-key";
	private static final String KEY_ATTR = "key";
	private static final String[] REGEX_PATH = {ELEM_NAME, "regex"};

	private static final String API_KEY_LABEL = "API Key";
	private static final String REGEX_LABEL =
			"Regular expressions (only request" + " matching any of following regular expressions will be allowed, " +
					"if no regular expression is set then request to any path is allowed)";
	private static final String DOMAIN_LABEL = "Domains for which this key works";
	@ConfigField(desc="Domains")
	private HashSet<String> domains = new HashSet<String>();
	private String key;
	@ConfigField(desc="Regexs")
	private CopyOnWriteArrayList<Pattern> regexs = new CopyOnWriteArrayList<Pattern>();

	@Override
	public String getElemName() {
		return ELEM_NAME;
	}

	@Override
	protected void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void addCommandFields(Packet packet) {
		Command.addFieldValue(packet, API_KEY_LABEL, key == null ? UUID.randomUUID().toString() : key);
		List<String> regexValues = new ArrayList<String>();
		if (!regexs.isEmpty()) {
			for (Pattern regex : this.regexs) {
				regexValues.add(regex.pattern());
			}
		}
		Command.addFieldMultiValue(packet, REGEX_LABEL, regexValues);
		List<String> domainValues = new ArrayList<String>();
		if (!domainValues.isEmpty()) {
			domainValues.addAll(domains);
		}
		Command.addFieldMultiValue(packet, DOMAIN_LABEL, domainValues);
	}

	@Override
	public void initFromCommand(Packet packet) {
		super.initFromCommand(packet);

		key = Command.getFieldValue(packet, API_KEY_LABEL);
		String[] regexs = Command.getFieldValues(packet, REGEX_LABEL);
		if (regexs != null) {
			for (String regex : regexs) {
				if (regex.isEmpty()) {
					continue;
				}
				this.regexs.add(Pattern.compile(regex));
			}
		}
		String[] domains = Command.getFieldValues(packet, DOMAIN_LABEL);
		if (domains != null) {
			for (String domain : domains) {
				this.domains.add(domain);
			}
		}
	}

	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != ELEM_NAME) {
			throw new IllegalArgumentException("Incorrect element name, expected: " + ELEM_NAME);
		}

		super.initFromElement(elem);
		key = elem.getAttributeStaticStr(KEY_ATTR);
		List<Element> children = elem.getChildren();
		if (children != null) {
			for (Element child : children) {
				if (child.getName().equals("domain")) {
					this.domains.add(tigase.xml.XMLUtils.unescape(child.getCData()));
				}
				if (child.getName().equals("regex")) {
					this.regexs.add(Pattern.compile(tigase.xml.XMLUtils.unescape(child.getCData())));
				}
			}
		}
	}

	@Override
	public void initFromPropertyString(String propString) {
		String[] props = propString.split(":");
		key = props[0];
		for (String prop : props) {
			if (prop.startsWith("domain")) {
				String[] v = prop.split("=");
				if (!v[1].isEmpty()) {
					this.domains.addAll(Arrays.asList(v[1].split(";")));
				}
			}
			if (prop.startsWith("regex")) {
				String[] v = prop.split("=");
				if (!v[1].isEmpty()) {
					Arrays.asList(v[1].split(";")).forEach(regex -> regexs.add(Pattern.compile(regex)));
				}
			}
		}
	}

	@Override
	public Element toElement() {
		Element elem = super.toElement();
		elem.setAttribute(KEY_ATTR, key);
		for (Pattern regex : regexs) {
			String pattern = regex.pattern();
			elem.addChild(new Element("regex", tigase.xml.XMLUtils.escape(pattern)));
		}
		for (String domain : domains) {
			elem.addChild(new Element("domain", tigase.xml.XMLUtils.escape(domain)));
		}
		return elem;
	}

	@Override
	public String toPropertyString() {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		if (!this.domains.isEmpty()) {
			sb.append(":domain=");
			boolean first = true;
			for (String domain : domains) {
				if (!first) {
					sb.append(";");
				} else {
					first = false;
				}
				sb.append(domain);
			}
		}
		if (!this.regexs.isEmpty()) {
			sb.append(":regex=");
			boolean first = true;
			for (Pattern regex : regexs) {
				if (!first) {
					sb.append(";");
				} else {
					first = false;
				}
				sb.append(regex.pattern());
			}
		}
		return sb.toString();
	}

	public boolean isAllowed(String key, String path) {
		return this.isAllowed(key, "default", path);
	}

	public boolean isAllowed(String key, String domain, String path) {
		if (this.key.equals(key)) {
			if (!this.domains.isEmpty()) {
				if (!this.domains.contains(domain)) {
					return false;
				}
			}
			if (this.regexs.isEmpty()) {
				return true;
			}
			for (Pattern regex : regexs) {
				if (regex.matcher(path).matches()) {
					return true;
				}
			}
		}
		return false;
	}

}
