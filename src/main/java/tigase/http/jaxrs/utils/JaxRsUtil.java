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
package tigase.http.jaxrs.utils;

import jakarta.ws.rs.container.AsyncResponse;
import jakarta.xml.bind.ValidationException;
import tigase.http.api.HttpException;
import tigase.http.util.XmppException;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

public class JaxRsUtil {

	public static Collection createCollectionInstance(Class<Collection> collectionClass)
			throws NoSuchMethodException, InvocationTargetException, InstantiationException,
				   IllegalAccessException {
		if (Modifier.isAbstract(collectionClass.getModifiers()) || Modifier.isInterface(collectionClass.getModifiers())) {
			if (List.class.isAssignableFrom(collectionClass)) {
				return new ArrayList();
			} else if (Set.class.isAssignableFrom(collectionClass)) {
				return new HashSet();
			} else {
				throw new InstantiationException("Unsupported collection type: " + collectionClass.getName());
			}
		} else {
			return collectionClass.getDeclaredConstructor().newInstance();
		}
	}

	public static HttpException convertExceptionForResponse(Throwable ex) {
		if (ex instanceof CompletionException) {
			return convertExceptionForResponse(ex.getCause());
		}
		else if (ex instanceof HttpException) {
			return (HttpException) ex;
		}
		else if (ex instanceof TimeoutException) {
			return new HttpException(HttpServletResponse.SC_REQUEST_TIMEOUT, ex.getCause());
		} else if (ex instanceof XmppException) {
			int code = ((XmppException) ex).getCode();
			if (code == 0) {
				code = SC_INTERNAL_SERVER_ERROR;
			}
			String message = Optional.ofNullable(ex.getMessage()).map(msg -> msg + "\n\n").orElse("") +
					((XmppException) ex).getStanza().toString();
			return new HttpException(message,  code);
		}
		else if (ex instanceof ValidationException) {
			return new HttpException(ex.getMessage(), HttpServletResponse.SC_NOT_ACCEPTABLE, ex);
		}
		else if (ex instanceof InvocationTargetException || ex instanceof IllegalAccessException) {
			return convertExceptionForResponse(ex.getCause());
		} else {
			return new HttpException("Internal Server Error", SC_INTERNAL_SERVER_ERROR, ex);
		}
	}

	public static void sendResult(CompletableFuture future, AsyncResponse asyncResponse) {
		future.thenAccept(asyncResponse::resume).exceptionally(ex -> {
			asyncResponse.resume(convertExceptionForResponse((Throwable) ex));
			return null;
		});
	}
}
