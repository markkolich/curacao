/*
 * Copyright (c) 2023 Mark S. Kolich
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

package curacao.servlet.jakarta;

import curacao.core.servlet.HttpCookie;
import curacao.core.servlet.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class JakartaHttpResponse implements HttpResponse {

    private final jakarta.servlet.http.HttpServletResponse delegate_;

    public JakartaHttpResponse(
            final jakarta.servlet.http.HttpServletResponse delegate) {
        delegate_ = checkNotNull(delegate, "Servlet response delegate cannot be null.");
    }

    @Override
    public jakarta.servlet.http.HttpServletResponse getDelegate() {
        return delegate_;
    }

    @Override
    public void setStatus(
            final int sc) {
        delegate_.setStatus(sc);
    }

    @Override
    public void setContentType(
            final String type) {
        delegate_.setContentType(type);
    }

    @Override
    public void setHeader(
            final String name,
            final String value) {
        delegate_.setHeader(name, value);
    }

    @Override
    public void addHeader(
            final String name,
            final String value) {
        delegate_.addHeader(name, value);
    }

    @Override
    public void addCookie(
            final HttpCookie cookie) {
        final Object delegate = cookie.getDelegate();
        if (delegate instanceof jakarta.servlet.http.Cookie) {
            delegate_.addCookie((jakarta.servlet.http.Cookie) delegate);
        } else {
            throw new UnsupportedOperationException("Unsupported cookie type: "
                    + delegate.getClass().getCanonicalName());
        }
    }

    @Override
    public void sendRedirect(
            final String location) throws IOException {
        delegate_.sendRedirect(location);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return delegate_.getOutputStream();
    }

    @Override
    public Writer getWriter() throws IOException {
        return delegate_.getWriter();
    }

}
