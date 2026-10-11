/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package curacao.util.http;

import com.google.common.collect.ImmutableList;
import curacao.core.servlet.HttpCookie;
import curacao.core.servlet.HttpResponse;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CookieBakerTest {

    private static final class TestCookie implements HttpCookie {

        private final String name_;
        private String value_;
        private String domain_;
        private int maxAge_ = -1;
        private String path_;
        private boolean secure_;
        private boolean httpOnly_;

        private TestCookie(
                final String name,
                final String value) {
            name_ = name;
            value_ = value;
        }

        @Override
        public Object getDelegate() {
            return this;
        }

        @Override
        public String getName() {
            return name_;
        }

        @Override
        public String getValue() {
            return value_;
        }

        @Override
        public void setValue(
                final String newValue) {
            value_ = newValue;
        }

        @Override
        public void setDomain(
                final String domain) {
            domain_ = domain;
        }

        @Override
        public int getMaxAge() {
            return maxAge_;
        }

        @Override
        public void setMaxAge(
                final int maxAge) {
            maxAge_ = maxAge;
        }

        @Override
        public String getPath() {
            return path_;
        }

        @Override
        public void setPath(
                final String path) {
            path_ = path;
        }

        @Override
        public boolean isSecure() {
            return secure_;
        }

        @Override
        public void setSecure(
                final boolean isSecure) {
            secure_ = isSecure;
        }

        @Override
        public boolean isHttpOnly() {
            return httpOnly_;
        }

        @Override
        public void setHttpOnly(
                final boolean httpOnly) {
            httpOnly_ = httpOnly;
        }

    }

    private static final class TestResponse implements HttpResponse {

        private final List<HttpCookie> cookies_ = new ArrayList<>();

        @Override
        public Object getDelegate() {
            return this;
        }

        @Override
        public void setStatus(
                final int sc) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setContentType(
                final String type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setHeader(
                final String name,
                final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addHeader(
                final String name,
                final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader(
                final String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addCookie(
                final HttpCookie cookie) {
            cookies_.add(cookie);
        }

        @Override
        public void sendRedirect(
                final String location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Writer getWriter() {
            throw new UnsupportedOperationException();
        }

    }

    @Test
    public void bakeTest() {
        final TestCookie cookie = bake(new CookieBaker.Builder(TestCookie::new)
                .setName("session")
                .setValue("abc")
                .setDomain("example.com")
                .setContextPath("/app")
                .setMaxAge(3600)
                .setSecure(true)
                .build());

        assertEquals("session", cookie.getName());
        assertEquals("abc", cookie.getValue());
        assertEquals("example.com", cookie.domain_);
        assertEquals("/app", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge());
        assertTrue(cookie.isSecure());
        // HttpOnly defaults to true.
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    public void bakeDefaultsTest() {
        final TestCookie cookie = bake(new CookieBaker.Builder(TestCookie::new)
                .setName("session")
                .setMaxAge(-5)
                .setHttpOnly(false)
                .build());

        // A non-positive max age expires the cookie; unset attributes are left as the factory made them.
        assertEquals(0, cookie.getMaxAge());
        assertNull(cookie.getValue());
        assertNull(cookie.domain_);
        assertNull(cookie.getPath());
        assertFalse(cookie.isSecure());
        assertFalse(cookie.isHttpOnly());
    }

    @Test
    public void bakeRequiresAFactoryCookieTest() {
        final CookieBaker nullCookie = new CookieBaker.Builder((name, value) -> null).setName("session").build();

        assertThrows(NullPointerException.class, () -> new CookieBaker.Builder(null));
        assertThrows(NullPointerException.class, () -> nullCookie.bake(new TestResponse()));
        assertThrows(NullPointerException.class, () -> new CookieBaker.Builder(TestCookie::new).build());
    }

    @Test
    public void getCookiesByNameTest() {
        final HttpCookie first = new TestCookie("a", "1");
        final HttpCookie second = new TestCookie("a", "2");
        final List<HttpCookie> cookies = ImmutableList.of(first, new TestCookie("b", "3"), second);

        assertEquals(ImmutableList.of(first, second), CookieBaker.getCookiesByName(cookies, "a"));
        assertSame(first, CookieBaker.getFirstCookieByName(cookies, "a"));
        assertNull(CookieBaker.getFirstCookieByName(cookies, "missing"));
        assertTrue(CookieBaker.getCookiesByName((List<HttpCookie>) null, "a").isEmpty());
    }

    private static TestCookie bake(
            final CookieBaker baker) {
        final TestResponse response = new TestResponse();
        baker.bake(response);

        assertEquals(1, response.cookies_.size());
        return (TestCookie) response.cookies_.get(0);
    }

}
