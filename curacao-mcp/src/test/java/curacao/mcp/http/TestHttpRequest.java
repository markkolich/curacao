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

package curacao.mcp.http;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import curacao.core.servlet.HttpCookie;
import curacao.core.servlet.HttpRequest;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * A POST with headers and nothing else; header names are case-insensitive, as in a servlet
 * container.
 */
/* package */ final class TestHttpRequest implements HttpRequest {

    private final ListMultimap<String, String> headers_ = LinkedListMultimap.create();

    /**
     * A request from a local, non-browser client: {@code Host: localhost:8080} and JSON.
     */
    /* package */ static TestHttpRequest local() {
        return new TestHttpRequest()
                .header("Host", "localhost:8080")
                .header("Content-Type", "application/json");
    }

    /* package */ TestHttpRequest header(
            final String name,
            final String value) {
        headers_.put(name, value);
        return this;
    }

    /* package */ TestHttpRequest without(
            final String name) {
        headers_.keySet().removeIf(key -> key.equalsIgnoreCase(name));
        return this;
    }

    @Override
    public Object getDelegate() {
        return null;
    }

    @Override
    public Object getAttribute(
            final String name) {
        return null;
    }

    @Override
    public String getMethod() {
        return "POST";
    }

    @Override
    public List<String> getHeaderNames() {
        return ImmutableList.copyOf(headers_.keySet());
    }

    @Override
    public List<String> getHeaders(
            final String name) {
        final ImmutableList.Builder<String> values = ImmutableList.builder();
        headers_.entries().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)))
                .forEach(entry -> values.add(entry.getValue()));

        return values.build();
    }

    @Override
    public String getHeader(
            final String name) {
        final List<String> values = getHeaders(name);

        return values.isEmpty() ? null : values.get(0);
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer("http://localhost:8080/mcp");
    }

    @Override
    public String getRequestURI() {
        return "/mcp";
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getParameter(
            final String name) {
        return null;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public List<HttpCookie> getCookies() {
        return ImmutableList.of();
    }

    @Override
    public int getContentLength() {
        return -1;
    }

    @Override
    public long getContentLengthLong() {
        return -1L;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return InputStream.nullInputStream();
    }

}
