/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.http.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import tigase.db.comp.RepositoryItemAbstract;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;

/**
 *
 * @author andrzej
 */
public class ApiKeyItem extends RepositoryItemAbstract {

	private static final String ELEM_NAME = "api-key";
	private static final String KEY_ATTR = "key";
	private static final String[] REGEX_PATH = { ELEM_NAME, "regex" };
	
	private static final String API_KEY_LABEL = "API Key";
	private static final String REGEX_LABEL = "Regular expressions (only request"
			+ " matching any of following regular expressions will be allowed, "
			+ "if no regular expression is set then request to any path is allowed)";
			
	
	private String key;
	private List<Pattern> regexs = new CopyOnWriteArrayList<Pattern>();
	
	@Override
	public String getElemName() {
		return ELEM_NAME;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void addCommandFields(Packet packet) {
		Command.addFieldValue(packet, API_KEY_LABEL, 
				key == null ? UUID.randomUUID().toString() : key);
		List<String> values = new ArrayList<String>();
		if (!regexs.isEmpty()) {			
			for (Pattern regex : this.regexs) {				
				values.add(regex.pattern());
			}
		}
		Command.addFieldMultiValue(packet, REGEX_LABEL, values);
	}
	
	@Override
	public void initFromCommand(Packet packet) {
		super.initFromCommand(packet);
		
		key = Command.getFieldValue(packet, API_KEY_LABEL);
		String[] regexs = Command.getFieldValues(packet, REGEX_LABEL);
		if (regexs != null) {
			for (String regex : regexs) {
				if (regex.isEmpty())
					continue;
				this.regexs.add(Pattern.compile(regex));
			}			
		}
	}
	
	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != ELEM_NAME)
			throw new IllegalArgumentException("Incorrect element name, expected: " +
					ELEM_NAME);
		
		super.initFromElement(elem);
		key = elem.getAttributeStaticStr(KEY_ATTR);
		List<Element> regexs = elem.getChildren();
		if (regexs != null) {
			for (Element regex : regexs) {
				if (!regex.getName().equals("regex"))
					continue;
				this.regexs.add(Pattern.compile(tigase.xml.XMLUtils.unescape(regex.getCData())));
			}
		}
	}
	
	@Override
	public void initFromPropertyString(String propString) {
		key = propString;
	}

	@Override
	public Element toElement() {
		Element elem =  super.toElement();
		elem.setAttribute(KEY_ATTR, key);
		for (Pattern regex : regexs) {
			String pattern = regex.pattern();
			elem.addChild(new Element("regex", tigase.xml.XMLUtils.escape(pattern)));
		}
		return elem;
	}
	
	@Override
	public String toPropertyString() {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		return sb.toString();
	}
	
	public boolean isAllowed(String key, String path) {
		if (this.key.equals(key)) {
			if (this.regexs.isEmpty())
				return true;
			for (Pattern regex : regexs) {
				if (regex.matcher(path).matches())
					return true;
			}
		}
		return false;
	}
	
}
