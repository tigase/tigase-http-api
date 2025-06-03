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

import tigase.http.jaxrs.validators.ConstraintViolation;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtendedValidationException extends ValidationException {

	private final List<ConstraintViolation> constraintViolations;
	private final List<ValidationException> parsingExceptions;

	public ExtendedValidationException(Collection<ConstraintViolation> constraintViolations, Collection<ValidationException> parsingExceptions) {
		super(Stream.concat(parsingExceptions.stream().map(ValidationException::getMessage), constraintViolations.stream().map(ConstraintViolation::getMessage)).collect(
				Collectors.joining("\n")));

		this.constraintViolations = new ArrayList<>(constraintViolations);
		this.parsingExceptions = new ArrayList<>(parsingExceptions);
	}

	public List<ConstraintViolation> getConstraintViolations() {
		return constraintViolations;
	}

	public List<ValidationException> getParsingExceptions() {
		return parsingExceptions;
	}
}
