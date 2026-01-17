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

package curacao.core.servlet;

import com.google.common.collect.ImmutableMultimap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface HttpRequest {

    Object getDelegate();

    Object getAttribute(
            final String name);

    String getMethod();

    List<String> getHeaderNames();

    List<String> getHeaders(
            final String name);

    default Map<String, Collection<String>> getHeaders() {
        final ImmutableMultimap.Builder<String, String> headerBuilder = ImmutableMultimap.builder();

        final List<String> headerNames = getHeaderNames();
        for (final String headerName : headerNames) {
            final List<String> headers = getHeaders(headerName);
            headerBuilder.putAll(headerName, headers);
        }

        return headerBuilder.build().asMap();
    }

    String getHeader(
            final String name);

    StringBuffer getRequestURL();

    String getRequestURI();

    String getQueryString();

    String getParameter(
            final String name);

    String getPathInfo();

    List<HttpCookie> getCookies();

    int getContentLength();

    long getContentLengthLong();

    String getCharacterEncoding();

    InputStream getInputStream() throws IOException;

}
