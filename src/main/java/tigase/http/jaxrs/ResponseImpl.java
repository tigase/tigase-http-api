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
package tigase.http.jaxrs;

import jakarta.ws.rs.core.*;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResponseImpl
		extends jakarta.ws.rs.core.Response {

	private final Object entity;
	private final int status;
	private final String reasonPhase;
	private final Headers<Object> metadata;

	public ResponseImpl(int status, String reasonPhase, Headers<Object> metadata, Object entity) {
		this.status = status;
		this.reasonPhase = reasonPhase;
		this.metadata = metadata;
		this.entity = entity;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public StatusType getStatusInfo() {
		return new StatusType() {
			@Override
			public int getStatusCode() {
				return status;
			}

			@Override
			public Status.Family getFamily() {
				return Status.Family.familyOf(status);
			}

			@Override
			public String getReasonPhrase() {
				return reasonPhase;
			}
		};
	}

	@Override
	public Object getEntity() {
		return entity;
	}

	@Override
	public <T> T readEntity(Class<T> aClass) {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public <T> T readEntity(GenericType<T> genericType) {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public <T> T readEntity(Class<T> aClass, Annotation[] annotations) {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public <T> T readEntity(GenericType<T> genericType, Annotation[] annotations) {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public boolean hasEntity() {
		return entity != null;
	}

	@Override
	public boolean bufferEntity() {
		return false;
	}

	@Override
	public void close() {

	}

	@Override
	public MediaType getMediaType() {
		Object obj = metadata.getFirst(HttpHeaders.CONTENT_TYPE);
		if (obj instanceof MediaType) return (MediaType) obj;
		if (obj == null) return null;
		String str = obj.toString();
		int idx = str.indexOf(";");
		String fullType = idx < 0 ? str : str.substring(0, idx);
		if (idx < 0) {
			MediaType decodedType = switch (fullType) {
				case MediaType.WILDCARD -> MediaType.WILDCARD_TYPE;
				case MediaType.APPLICATION_JSON -> MediaType.APPLICATION_JSON_TYPE;
				case MediaType.APPLICATION_XML ->  MediaType.APPLICATION_XML_TYPE;
				case MediaType.TEXT_HTML -> MediaType.TEXT_HTML_TYPE;
				default -> null;
			};
			if (decodedType != null) {
				return decodedType;
			}
		}

		String[] parts = fullType.split("/");
		if (parts.length != 2) {
			return null;
		}

		return MediaType.valueOf(toHeaderString(obj));
	}

	@Override
	public Locale getLanguage() {
		Object obj = metadata.getFirst(HttpHeaders.CONTENT_LANGUAGE);
		if (obj instanceof Locale) return (Locale) obj;
		if (obj == null) return null;
		return Locale.forLanguageTag(obj.toString());
	}

	@Override
	public int getLength() {
		return -1;
	}

	@Override
	public Set<String> getAllowedMethods() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public Map<String, NewCookie> getCookies() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public EntityTag getEntityTag() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public Date getDate() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public Date getLastModified() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public URI getLocation() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public Set<Link> getLinks() {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public boolean hasLink(String s) {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public Link getLink(String s) {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public Link.Builder getLinkBuilder(String s) {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	@Override
	public MultivaluedMap<String, Object> getMetadata() {
		return metadata;
	}

	@Override
	public MultivaluedMap<String, String> getStringHeaders() {
		Headers<String> headers = new Headers<>();
		for (Map.Entry<String,List<Object>> entry : metadata.entrySet()) {
			for (Object it : entry.getValue()) {
				headers.add(entry.getKey(), toHeaderString(it));
			}
		}
		return headers;
	}

	@Override
	public String getHeaderString(String s) {
		throw new UnsupportedOperationException("Feature not implemented");
	}

	private String toHeaderString(Object object) {
		if (object == null) {
			return "";
		}
		if (object instanceof String) {
			return (String) object;
		} else if (object instanceof MediaType) {
			MediaType type = (MediaType) object;
			StringBuilder sb = new StringBuilder();
			sb.append(type.getType()).append("/").append(type.getSubtype());
			for (Map.Entry<String, String> it : type.getParameters().entrySet()) {
				sb.append("; ").append(it.getKey()).append("=\"").append(it.getValue()).append("\"");
			}
			return sb.toString();
		} else if (object instanceof Date) {
			return ResponseBuilderImpl.getDateFormatRFC822().format((Date) object);
		}
		return object.toString();
	}

	public static class ResponseBuilderImpl
			extends jakarta.ws.rs.core.Response.ResponseBuilder {

		public static SimpleDateFormat getDateFormatRFC822() {
			SimpleDateFormat dateFormatRFC822 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			dateFormatRFC822.setTimeZone(TimeZone.getTimeZone("GMT"));
			return dateFormatRFC822;
		}

		private Object entity;
		private int status = -1;
		private String reasonPhase;
		private Headers<Object> metadata = new Headers<>();

		@Override
		public jakarta.ws.rs.core.Response build() {
			if (status == -1) {
				if (entity == null) {
					status = 204;
				} else {
					status = 200;
				}
			}
			return new ResponseImpl(status, reasonPhase, metadata, entity);
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder clone() {
			return null;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder status(int status) {
			return status(status, null);
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder status(int status, String reasonPhase) {
			this.status = status;
			this.reasonPhase = reasonPhase;
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder entity(Object entity) {
			this.entity = entity;
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder entity(Object o, Annotation[] annotations) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder allow(String... strings) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder allow(Set<String> set) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
			if (cacheControl == null)
			{
				metadata.remove(HttpHeaders.CACHE_CONTROL);
				return this;
			}
			metadata.putSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder encoding(String s) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder header(String name, Object value) {
			if (value == null) {
				metadata.remove(name);
				return this;
			}
			metadata.add(name, value);
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder replaceAll(MultivaluedMap<String, Object> multivaluedMap) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder language(String language) {
			if (language == null) {
				metadata.remove(HttpHeaders.CONTENT_LANGUAGE);
				return this;
			}
			metadata.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder language(Locale language) {
			if (language == null) {
				metadata.remove(HttpHeaders.CONTENT_LANGUAGE);
				return this;
			}
			metadata.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder type(MediaType mediaType) {
			if (mediaType == null) {
				metadata.remove(HttpHeaders.CONTENT_TYPE);
			} else {
				metadata.putSingle(HttpHeaders.CONTENT_TYPE, mediaType);
			}
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder type(String mediaType) {
			if (mediaType == null) {
				metadata.remove(HttpHeaders.CONTENT_TYPE);
			} else {
				metadata.putSingle(HttpHeaders.CONTENT_TYPE, mediaType);
			}
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder variant(Variant variant) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder contentLocation(URI uri) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder cookie(NewCookie... newCookies) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder expires(Date expires) {
			if (expires == null) {
				metadata.remove(HttpHeaders.EXPIRES);
				return this;
			}
			metadata.putSingle(HttpHeaders.EXPIRES, getDateFormatRFC822().format(expires));
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder lastModified(Date lastModified) {
			if (lastModified == null) {
				metadata.remove(HttpHeaders.LAST_MODIFIED);
				return this;
			}
			metadata.putSingle(HttpHeaders.LAST_MODIFIED, lastModified);
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder location(URI location) {
			if (location == null) {
				metadata.remove(HttpHeaders.LOCATION);
				return this;
			}
			if (!location.isAbsolute()) {
				throw new UnsupportedOperationException("Feature not implemented");
			}
			metadata.putSingle(HttpHeaders.LOCATION, location);
			return this;
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder tag(EntityTag entityTag) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder tag(String s) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder variants(Variant... variants) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder variants(List<Variant> list) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder links(Link... links) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder link(URI uri, String s) {
			throw new UnsupportedOperationException("Feature not implemented");
		}

		@Override
		public jakarta.ws.rs.core.Response.ResponseBuilder link(String s, String s1) {
			throw new UnsupportedOperationException("Feature not implemented");
		}
	}
}
