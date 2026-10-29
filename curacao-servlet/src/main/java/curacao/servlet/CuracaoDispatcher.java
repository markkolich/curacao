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

package curacao.servlet;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import curacao.exceptions.requests.ServiceUnavailableException;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Hands a request to Curacao's thread pool, for the dispatcher servlets, so that every request is answered even
 * while the context shuts down.
 */
public final class CuracaoDispatcher {

    private CuracaoDispatcher() {
    }

    /**
     * Runs the request's callable on the executor, and its callback with the result. If the executor has shut
     * down, the request is answered at once with a {@link ServiceUnavailableException} (a 503). If it shuts down
     * while the request runs, the callback renders the response on the thread that ran the request, rather than
     * the response being dropped and the request left to time out.
     */
    public static void submit(
            @Nonnull final ListeningExecutorService executor,
            @Nonnull final Callable<Object> callable,
            @Nonnull final FutureCallback<Object> callback) {
        checkNotNull(executor, "Executor service cannot be null.");
        checkNotNull(callable, "Request callable cannot be null.");
        checkNotNull(callback, "Response callback cannot be null.");

        final ListenableFuture<Object> future;
        try {
            future = executor.submit(callable);
        } catch (final RejectedExecutionException e) {
            callback.onFailure(new ServiceUnavailableException("Shutting down; not accepting requests.", e));
            return;
        }

        Futures.addCallback(future, callback, orRunInPlace(executor));
    }

    /**
     * Runs a command on the executor, or, once the executor has shut down, on the calling thread.
     */
    private static Executor orRunInPlace(
            final Executor executor) {
        return command -> {
            try {
                executor.execute(command);
            } catch (final RejectedExecutionException e) {
                command.run();
            }
        };
    }

}
