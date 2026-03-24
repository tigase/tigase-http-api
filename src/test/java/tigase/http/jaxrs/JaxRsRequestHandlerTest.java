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

import jakarta.ws.rs.PathParam;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static tigase.http.jaxrs.JaxRsRequestHandler.prepareMatcher;

public class JaxRsRequestHandlerTest {

    private static class Endpoints {
        public void getUser(@PathParam("id") Integer id) {}
        public void getOrder(@PathParam("userId") Long userId,
                             @PathParam("orderId") Long orderId) {}
        public void getCheckURL(@PathParam("restOfUrl") String restOfUrl) {}
    }

    private static Method method(String name) throws Exception {
        return Arrays.stream(Endpoints.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(name));
    }

    @Test
    public void singleParam_matchesCorrectValue() throws Exception {
        Pattern p = prepareMatcher("/users/{id}", method("getUser"));

        assertNotNull(p);
        Matcher m = p.matcher("/users/42");
        assertTrue(m.matches());
        assertEquals("42", m.group("id"));
    }

    @Test
    public void singleParam_rejectsTypeMismatch() throws Exception {
        Pattern p = prepareMatcher("/users/{id}", method("getUser"));

        assertNotNull(p);
        assertFalse(p.matcher("/users/abc").matches());
    }

    @Test
    public void multipleParams_capturesBoth() throws Exception {
        Pattern p = prepareMatcher("/users/{userId}/orders/{orderId}",
                method("getOrder"));

        assertNotNull(p);
        Matcher m = p.matcher("/users/1/orders/99");
        assertTrue(m.matches());
        assertEquals("1",  m.group("userId"));
        assertEquals("99", m.group("orderId"));
    }

    @Test
    public void testMissingClosingBrace_returnsNull() throws Exception {
        Pattern p = prepareMatcher("/users/{id", method("getUser"));
        assertNull(p);
    }

    @Test
    public void testUnknownParamName_returnsNull() throws Exception {
        Pattern p = prepareMatcher("/users/{unknown}", method("getUser"));
        assertNull(p);
    }

    @Test
    public void testLiteralsNotBeingParsedAsRegex_dotInVersion() throws Exception {
        Pattern p = prepareMatcher("/v1.0/users/{id}", method("getUser"));

        assertNotNull(p);
        assertTrue(p.matcher("/v1.0/users/7").matches());
        assertFalse("Must match `.` literal, not a regex wildcard", p.matcher("/v100/users/7").matches());
    }

    @Test
    public void testLiteralsNotBeingParsedAsRegex_plusInPath() throws Exception {
        Pattern p = prepareMatcher("/items+list/{id}", method("getUser"));

        assertNotNull(p);
        assertTrue(p.matcher("/items+list/1").matches());
        assertFalse("Must match `+` literal, not a regex quantifier",p.matcher("/itemslist/1").matches());
    }

    @Test
    public void inlineRegexInNamedParameterMatchAll() throws Exception {
        Pattern p = prepareMatcher("/rest/update/{restOfUrl : .*}", method("getCheckURL"));

        assertNotNull(p);
        Matcher m = p.matcher("/rest/update/files/downloads/tigase-server/descript-redmine.ion");
        assertTrue(m.matches());
        assertEquals("capture group name should be the param name without the inline regex", "files/downloads/tigase-server/descript-redmine.ion", m.group("restOfUrl"));
    }

    @Test
    public void inlineRegexInNamedParameterStringFilename() throws Exception {
        Pattern p = prepareMatcher("/rest/update/{restOfUrl : [a-zA-Z\\-\\.]+}", method("getCheckURL"));

        assertNotNull(p);
        Matcher m = p.matcher("/rest/update/descript-redmine.ion");
        assertTrue(m.matches());
        assertEquals("capture group name should be the param name without the inline regex", "descript-redmine.ion", m.group("restOfUrl"));
    }
}