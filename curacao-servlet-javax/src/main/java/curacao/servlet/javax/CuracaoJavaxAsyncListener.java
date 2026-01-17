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

import com.google.common.util.concurrent.FutureCallback;
import curacao.context.CuracaoContext;
import curacao.exceptions.async.AsyncException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CuracaoJavaxAsyncListener implements AsyncListener {

    private final CuracaoContext ctx_;

    private final FutureCallback<Object> callback_;

    public CuracaoJavaxAsyncListener(
            final CuracaoContext ctx,
            final FutureCallback<Object> callback) {
        ctx_ = checkNotNull(ctx, "Curacao context cannot be null.");
        callback_ = checkNotNull(callback, "Future callback cannot be null.");
    }

    @Override
    public void onComplete(
            final AsyncEvent event) throws IOException {
        // No-op
    }

    @Override
    public void onTimeout(
            final AsyncEvent event) throws IOException {
        Throwable cause = event.getThrowable();
        if (cause == null) {
            cause = new AsyncException.WithTimeout("Async context not completed "
                    + "within timeout: " + ctx_, ctx_);
        }
        callback_.onFailure(cause);
    }

    @Override
    public void onError(
            final AsyncEvent event) throws IOException {
        Throwable cause = event.getThrowable();
        if (cause == null) {
            cause = new AsyncException.WithError("Async context error: "
                    + ctx_, ctx_);
        }
        callback_.onFailure(cause);
    }

    @Override
    public void onStartAsync(
            final AsyncEvent event) throws IOException {
        // No-op
    }

}
