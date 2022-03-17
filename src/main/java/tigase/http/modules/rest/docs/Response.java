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
package tigase.http.modules.rest.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Produces;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

public class Response
		extends Payload {

	public static List<Response> create(Method method) {
		Optional<Operation> operation = Optional.ofNullable(method.getAnnotation(Operation.class));

		List<ApiResponse> apiResponses = new ArrayList<>();
		Optional.ofNullable(method.getAnnotation(ApiResponses.class))
				.ifPresent(responses -> Arrays.stream(responses.value()).forEach(apiResponses::add));
		Optional.ofNullable(method.getAnnotation(ApiResponse.class)).ifPresent(apiResponses::add);

		List<Response> responses = new ArrayList<>();
		Optional<ApiResponse> successResponse = apiResponses.stream().filter(r -> r.responseCode().startsWith("2")).findFirst();
		Stream<ApiResponse> apiResponseStream = apiResponses.stream();
		if (successResponse.isPresent()) {
			List<PayloadExample> payloadExamples = new ArrayList<>();
			Model model = null;
			for (Content content : successResponse.get().content()) {
				Class returnType = content.schema().implementation();
				if (!void.class.equals(returnType)) {
					if (content.mediaType().equals("")) {
						Produces produces = method.getAnnotation(Produces.class);
						if (produces != null) {
							payloadExamples.addAll(PayloadExample.create(returnType, produces.value()));
						}
					} else {
						Optional.ofNullable(PayloadExample.create(returnType, content.mediaType())).ifPresent(payloadExamples::add);
					}
					model = Model.create(returnType);
				}
			}
			responses.add(new Response(successResponse.get().responseCode(), successResponse.get().description(), model, payloadExamples));
			apiResponseStream = apiResponseStream.filter(r -> !r.responseCode().startsWith("2"));
		} else {
			Produces produces = method.getAnnotation(Produces.class);
			if (produces != null) {
				Class returnType = method.getReturnType();
				if (!(void.class.equals(returnType) || Response.class.isAssignableFrom(returnType))) {
					List<PayloadExample> payloadExamples = PayloadExample.create(method.getReturnType(), produces.value());
					responses.add(new Response("200", null, Model.create(returnType), payloadExamples));
				}
			}
		}
		apiResponseStream.map(r -> new Response(r.responseCode(), r.description(), null, Collections.emptyList())).forEach(responses::add);

		return responses;
	}

	private final String code;
	private final String description;

	public Response(String code, String description, Model model,
					List<PayloadExample> payloadExamples) {
		super(model, payloadExamples);
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

}
