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
import com.google.common.util.concurrent.ListeningExecutorService;
import curacao.exceptions.requests.ServiceUnavailableException;
import curacao.util.AsyncExecutorServiceFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CuracaoDispatcherTest {

    @Test
    public void answersARequestAfterShutdownWithA503Test() throws Exception {
        final ListeningExecutorService executor =
                AsyncExecutorServiceFactory.createNewListeningExecutorService(1, "dispatch-test-%d");
        executor.shutdown();

        final RecordingCallback callback = new RecordingCallback();
        CuracaoDispatcher.submit(executor, () -> "never", callback);

        // Answered at once, on the calling thread.
        assertTrue(callback.outcome_.isDone());
        assertInstanceOf(ServiceUnavailableException.class, callback.outcome_.get());
    }

    @Test
    public void answersARequestInFlightDuringShutdownTest() throws Exception {
        final ListeningExecutorService executor =
                AsyncExecutorServiceFactory.createNewListeningExecutorService(1, "dispatch-test-%d");
        final CountDownLatch running = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        final RecordingCallback callback = new RecordingCallback();
        CuracaoDispatcher.submit(executor, () -> {
            running.countDown();
            release.await();
            return "done";
        }, callback);
        assertTrue(running.await(5L, TimeUnit.SECONDS));

        // The pool shuts down while the request runs; its response is still rendered.
        executor.shutdown();
        release.countDown();

        assertEquals("done", callback.outcome_.get(5L, TimeUnit.SECONDS));
    }

    /**
     * Records the result, or the failure, that a request ended with.
     */
    private static final class RecordingCallback implements FutureCallback<Object> {

        private final CompletableFuture<Object> outcome_ = new CompletableFuture<>();

        @Override
        public void onSuccess(
                final Object result) {
            outcome_.complete(result);
        }

        @Override
        public void onFailure(
                final Throwable t) {
            outcome_.complete(t);
        }

    }

}
