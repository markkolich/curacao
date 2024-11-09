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

package curacao.servlet.jakarta;

import curacao.core.servlet.ServletContext;

import static com.google.common.base.Preconditions.checkNotNull;

public final class JakartaServletContext implements ServletContext {

    private final jakarta.servlet.ServletContext delegate_;

    public JakartaServletContext(
            final jakarta.servlet.ServletContext delegate) {
        delegate_ = checkNotNull(delegate, "Servlet context delegate cannot be null.");
    }

    @Override
    public jakarta.servlet.ServletContext getDelegate() {
        return delegate_;
    }

    @Override
    public String getContextPath() {
        return delegate_.getContextPath();
    }

    @Override
    public String getRealPath(
            final String path) {
        return delegate_.getRealPath(path);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(
            final String name) {
        return (T) delegate_.getAttribute(name);
    }

    @Override
    public void setAttribute(
            final String name,
            final Object object) {
        delegate_.setAttribute(name, object);
    }

}
