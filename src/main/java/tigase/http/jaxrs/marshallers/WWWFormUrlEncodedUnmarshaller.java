package tigase.http.jaxrs.marshallers;

import jakarta.ws.rs.FormParam;
import jakarta.xml.bind.UnmarshalException;
import tigase.http.jaxrs.RequestHandler;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;

public class WWWFormUrlEncodedUnmarshaller {

	public Object unmarshal(Class clazz, HttpServletRequest request) throws UnmarshalException {
		try {
			Object object = clazz.getDeclaredConstructor().newInstance();
			for (Field field : clazz.getDeclaredFields()) {
				FormParam formParam = field.getAnnotation(FormParam.class);
				if (formParam != null) {
					String[] valuesStr = request.getParameterValues(formParam.value());
					Object value = null;
					if (boolean.class.equals(field.getType())) {
						value = valuesStr != null && valuesStr.length == 1 && "on".equals(valuesStr[0]);
					} else {
						if (valuesStr != null) {
							value = RequestHandler.convertToValue(field.getGenericType(), valuesStr);
						}
					}
					field.setAccessible(true);
					field.set(object, value);
				}
			}
			return object;
		} catch (Throwable ex) {
			throw new UnmarshalException("Could not decode " + clazz.getCanonicalName() + " from submitted form");
		}
	}

}
