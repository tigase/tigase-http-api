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
package tigase.http.jaxrs.converters;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import tigase.xmpp.jid.JID;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class JIDParamConverterProvider
		implements ParamConverterProvider {

	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type type, Annotation[] annotations) {
		if (rawType == JID.class) {
			return (ParamConverter<T>) new JIDParamConverter();
		}
		return null;
	}

	public static class JIDParamConverter implements ParamConverter<JID> {

		@Override
		public JID fromString(String s) {
			if (s == null) {
				return null;
			}
			return JID.jidInstanceNS(s);
		}

		@Override
		public String toString(JID jid) {
			if (jid == null) {
				return null;
			}
			return jid.toString();
		}
	}
}
