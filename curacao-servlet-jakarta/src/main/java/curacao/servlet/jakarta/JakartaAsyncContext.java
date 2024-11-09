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

import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpRequest;
import curacao.core.servlet.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;

public final class JakartaAsyncContext implements AsyncContext {

    private final jakarta.servlet.AsyncContext delegate_;

    public JakartaAsyncContext(
            final jakarta.servlet.AsyncContext delegate) {
        delegate_ = checkNotNull(delegate, "Async context delegate cannot be null.");
    }

    @Override
    public jakarta.servlet.AsyncContext getDelegate() {
        return delegate_;
    }

    @Override
    public HttpRequest getRequest() {
        return new JakartaHttpRequest((HttpServletRequest) delegate_.getRequest());
    }

    @Override
    public HttpResponse getResponse() {
        return new JakartaHttpResponse((HttpServletResponse) delegate_.getResponse());
    }

    @Override
    public void setTimeout(
            final long timeout) {
        delegate_.setTimeout(timeout);
    }

    @Override
    public long getTimeout() {
        return delegate_.getTimeout();
    }

    @Override
    public void dispatch(
            final String path) {
        delegate_.dispatch(path);
    }

    @Override
    public void dispatch() {
        delegate_.dispatch();
    }

    @Override
    public void complete() {
        delegate_.complete();
    }

}
