package tigase.http.jaxrs.converters;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import tigase.xmpp.jid.BareJID;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class BareJIDParamConvereterProvider implements ParamConverterProvider {

	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type type, Annotation[] annotations) {
		if (rawType == BareJID.class) {
			return (ParamConverter<T>) new BareJIDParamConverter();
		}
		return null;
	}

	public static class BareJIDParamConverter implements ParamConverter<BareJID> {

		@Override
		public BareJID fromString(String s) {
			if (s == null) {
				return null;
			}
			return BareJID.bareJIDInstanceNS(s);
		}

		@Override
		public String toString(BareJID bareJID) {
			if (bareJID == null) {
				return null;
			}
			return bareJID.toString();
		}
	}

}
