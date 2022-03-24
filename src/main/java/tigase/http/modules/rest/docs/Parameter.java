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

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.SecurityContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Parameter
		extends Payload {

	public static List<Parameter> create(Method method) {
		List<Parameter> params = new ArrayList<>();
		for (java.lang.reflect.Parameter param : method.getParameters()) {
			if (param.getAnnotation(Suspended.class) != null || AsyncResponse.class.isAssignableFrom(param.getType())) {
				continue;
			}
			if (HttpServletRequest.class.isAssignableFrom(param.getType()) || HttpServletResponse.class.isAssignableFrom(param.getType())) {
				continue;
			}

			String name = "body";
			Parameter.ParameterKind kind = Parameter.ParameterKind.body;
			String type = "object";
			String description = null;
			Model model = null;
			List<PayloadExample> payloadExamples = new ArrayList<>();

			boolean required = param.getAnnotation(NotNull.class) != null;
			PathParam pathParam = param.getAnnotation(PathParam.class);
			HeaderParam headerParam = param.getAnnotation(HeaderParam.class);
			FormParam formParam = param.getAnnotation(FormParam.class);
			BeanParam beanParam = param.getAnnotation(BeanParam.class);
			if (pathParam != null) {
				name = pathParam.value();
				type = param.getType().getSimpleName();
				kind = ParameterKind.path;
			} else if (headerParam != null) {
				name = headerParam.value();
				type = param.getType().getSimpleName();
				kind = ParameterKind.header;
			} else if (formParam != null) {
				name = formParam.value();
				type = param.getType().getSimpleName();
				kind = ParameterKind.formData;
			} else if (beanParam != null) {
				params.addAll(getBeanParameters(param.getType()));
				continue;
			} else if (SecurityContext.class.isAssignableFrom(param.getType())) {
				name = "principal";
				type = "BareJID";
				kind = ParameterKind.authorizedUser;
				description = "Bare JID of the authorized user";
			} else {
				Consumes consumes = method.getAnnotation(Consumes.class);
				if (consumes != null) {
					payloadExamples.addAll(PayloadExample.create(param.getType(), consumes.value()));
				}
				model = Model.create(param.getType());
			}

			List<String> possibleValues = null;
			if (Enum.class.isAssignableFrom(param.getType())) {
				possibleValues = Arrays.stream(param.getType().getEnumConstants())
						.map(Enum.class::cast)
						.map(Enum::name)
						.collect(Collectors.toList());
			}

			io.swagger.v3.oas.annotations.Parameter parameterAnnotation = param.getAnnotation(
					io.swagger.v3.oas.annotations.Parameter.class);
			if (parameterAnnotation != null) {
				if (!parameterAnnotation.name().isEmpty()) {
					name = parameterAnnotation.name();
				}
				description = parameterAnnotation.description();
			}

			params.add(new Parameter(name, required, kind, type, description, possibleValues, model, payloadExamples));
		}
		return params;
	}

	public static List<Parameter> getBeanParameters(Class<?> clazz) {
		List<Parameter> params = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			String name = null;
			ParameterKind kind = null;
			Class type = null;
			String description = null;
			List<String> possibleValues = null;

			boolean required = field.getAnnotation(NotNull.class) != null;
			PathParam pathParam = field.getAnnotation(PathParam.class);
			HeaderParam headerParam = field.getAnnotation(HeaderParam.class);
			FormParam formParam = field.getAnnotation(FormParam.class);
			if (pathParam != null) {
				name = pathParam.value();
				type = field.getType();
				kind = ParameterKind.path;
			} else if (headerParam != null) {
				name = headerParam.value();
				type = field.getType();
				kind = ParameterKind.header;
			} else if (formParam != null) {
				name = formParam.value();
				type = field.getType();
				kind = ParameterKind.formData;
			} else {
				continue;
			}

			if (Enum.class.isAssignableFrom(type)) {
				possibleValues = Arrays.stream(type.getEnumConstants())
						.map(Enum.class::cast)
						.map(Enum::name)
						.collect(Collectors.toList());
			}

			io.swagger.v3.oas.annotations.Parameter parameterAnnotation = field.getAnnotation(
					io.swagger.v3.oas.annotations.Parameter.class);
			if (parameterAnnotation != null) {
				if (!parameterAnnotation.name().isEmpty()) {
					name = parameterAnnotation.name();
				}
				description = parameterAnnotation.description();
			}

			params.add(new Parameter(name, required, kind, type.getName(), description, possibleValues, null, Collections.emptyList()));
		}

		return params;
	}

	public enum ParameterKind {
		path,
		header,
		formData,
		body,
		authorizedUser
	}

	private final String description;

	private final String name;
	private final boolean required;
	private final ParameterKind kind;
	private final String type;
	private final List<String> possibleValues;

	public Parameter(String name, boolean required, ParameterKind kind, String type, String description,
					 List<String> possibleValues, Model model, List<PayloadExample> payloadExamples) {
		super(model, payloadExamples);
		this.name = name;
		this.required = required;
		this.kind = kind;
		this.type = type;
		this.description = description;
		this.possibleValues = possibleValues;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public boolean isRequired() {
		return required;
	}

	public ParameterKind getKind() {
		return kind;
	}

	public String getType() {
		return type;
	}

	public List<String> getPossibleValues() {
		return possibleValues;
	}
}
