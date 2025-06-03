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
package tigase.http.jaxrs.validators;

import tigase.http.jaxrs.annotations.JidLocalpart;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class JidLocalpartValidator implements ConstraintValidator<JidLocalpart, String> {

	@Override
	public void initialize(JidLocalpart constraintAnnotation) {
		ConstraintValidator.super.initialize(constraintAnnotation);
	}

	@Override
	public boolean isValid(String localpart, ConstraintValidatorContext constraintValidatorContext) {
		try {
			BareJID.bareJIDInstance(localpart, "domain.com");
			return true;
		} catch (TigaseStringprepException e) {
			return false;
		}
	}
}
