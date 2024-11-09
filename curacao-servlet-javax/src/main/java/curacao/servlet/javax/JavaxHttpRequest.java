/*
 * Copyright (c) 2024 Mark S. Kolich
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

package curacao.servlet.javax;

import com.google.common.collect.ImmutableList;
import curacao.core.servlet.HttpCookie;
import curacao.core.servlet.HttpRequest;
import org.apache.commons.lang3.ArrayUtils;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class JavaxHttpRequest implements HttpRequest {

    private final javax.servlet.http.HttpServletRequest delegate_;

    public JavaxHttpRequest(
            final javax.servlet.http.HttpServletRequest delegate) {
        delegate_ = checkNotNull(delegate, "Servlet request delegate cannot be null.");
    }

    @Override
    public javax.servlet.http.HttpServletRequest getDelegate() {
        return delegate_;
    }

    @Override
    public Object getAttribute(
            final String name) {
        return delegate_.getAttribute(name);
    }

    @Override
    public String getMethod() {
        return delegate_.getMethod();
    }

    @Override
    public List<String> getHeaderNames() {
        final ImmutableList.Builder<String> headerNamesBuilder = ImmutableList.builder();

        final Enumeration<String> headerNames = delegate_.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            headerNamesBuilder.add(headerNames.nextElement());
        }

        return headerNamesBuilder.build();
    }

    @Override
    public List<String> getHeaders(
            final String name) {
        final ImmutableList.Builder<String> headersBuilder = ImmutableList.builder();

        final Enumeration<String> headers = delegate_.getHeaders(name);
        while (headers != null && headers.hasMoreElements()) {
            headersBuilder.add(headers.nextElement());
        }

        return headersBuilder.build();
    }

    @Override
    public String getHeader(
            final String name) {
        return delegate_.getHeader(name);
    }

    @Override
    public StringBuffer getRequestURL() {
        return delegate_.getRequestURL();
    }

    @Override
    public String getRequestURI() {
        return delegate_.getRequestURI();
    }

    @Override
    public String getQueryString() {
        return delegate_.getQueryString();
    }

    @Override
    public String getParameter(
            final String name) {
        return delegate_.getParameter(name);
    }

    @Override
    public String getPathInfo() {
        return delegate_.getPathInfo();
    }

    @Override
    public List<HttpCookie> getCookies() {
        final Cookie[] cookies = delegate_.getCookies();
        if (ArrayUtils.isEmpty(cookies)) {
            return ImmutableList.of();
        }

        return Arrays.stream(cookies)
                .map(JavaxHttpCookie::new)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public int getContentLength() {
        return delegate_.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return delegate_.getContentLengthLong();
    }

    @Override
    public String getCharacterEncoding() {
        return delegate_.getCharacterEncoding();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate_.getInputStream();
    }

}
