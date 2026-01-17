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

package curacao.servlet.javax;

import curacao.core.servlet.HttpCookie;

import static com.google.common.base.Preconditions.checkNotNull;

public final class JavaxHttpCookie implements HttpCookie {

    private final javax.servlet.http.Cookie delegate_;

    public JavaxHttpCookie(
            final javax.servlet.http.Cookie delegate) {
        delegate_ = checkNotNull(delegate, "Cookie delegate cannot be null.");
    }

    public JavaxHttpCookie(
            final String name,
            final String value) {
        this(new javax.servlet.http.Cookie(name, value));
    }

    @Override
    public javax.servlet.http.Cookie getDelegate() {
        return delegate_;
    }

    @Override
    public String getName() {
        return delegate_.getName();
    }

    @Override
    public String getValue() {
        return delegate_.getValue();
    }

    @Override
    public void setValue(
            final String newValue) {
        delegate_.setValue(newValue);
    }

    @Override
    public void setDomain(
            final String domain) {
        delegate_.setDomain(domain);
    }

    @Override
    public int getMaxAge() {
        return delegate_.getMaxAge();
    }

    @Override
    public void setMaxAge(
            final int maxAge) {
        delegate_.setMaxAge(maxAge);
    }

    @Override
    public String getPath() {
        return delegate_.getPath();
    }

    @Override
    public void setPath(
            final String path) {
        delegate_.setPath(path);
    }

    @Override
    public boolean isSecure() {
        return delegate_.getSecure();
    }

    @Override
    public void setSecure(
            final boolean isSecure) {
        delegate_.setSecure(isSecure);
    }

    @Override
    public boolean isHttpOnly() {
        return delegate_.isHttpOnly();
    }

    @Override
    public void setHttpOnly(
            final boolean httpOnly) {
        delegate_.setHttpOnly(httpOnly);
    }

}
