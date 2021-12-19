/*
 * Copyright (c) 2021 Mark S. Kolich
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

package curacao.handlers;

import curacao.context.CuracaoContext;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractContextCompletingCallbackHandler extends AbstractFutureCallbackHandler {

    private static final Logger LOG = getLogger(AbstractContextCompletingCallbackHandler.class);

    private final AsyncContextState state_ = new AsyncContextState();

    public AbstractContextCompletingCallbackHandler(
            @Nonnull final CuracaoContext ctx) {
        super(ctx);
    }

    @Override
    public final void successAndComplete(
            @Nonnull final Object result) throws Exception {
        new AbstractAsyncCompletingCallbackWrapper() {
            @Override
            public void doit() throws Exception {
                renderSuccess(result);
            }

            @Override
            public void cant() {
                // You'd get here if the async context timed out, and the AsyncListener.onTimeout()
                // method was called to handle the timeout event (and render an error response). But,
                // at some point in the future a slow controller finishes and tries to complete the
                // context again after data has already been written out and the context completed.
                LOG.warn("On success and complete: attempted to start & render response after context "
                        + "state was already started; ignoring!");
            }
        }.start();
    }

    @Override
    public final void failureAndComplete(
            @Nonnull final Throwable t) throws Exception {
        new AbstractAsyncCompletingCallbackWrapper() {
            @Override
            public void doit() throws Exception {
                renderFailure(t);
            }

            @Override
            public void cant() {
                // You'd get here if the async context timed out, and the AsyncListener.onTimeout()
                // method was called to handle the timeout event (and render an error response). But,
                // at some point in the future a slow controller finishes and tries to complete the
                // context again after data has already been written out and the context completed.
                LOG.warn("On failure and complete: attempted to start & render response after context "
                        + "state was already started; ignoring!");
            }
        }.start();
    }

    public abstract void renderSuccess(
            @Nonnull final Object result) throws Exception;

    public abstract void renderFailure(
            @Nonnull final Throwable t) throws Exception;

    /**
     * An internal class used to model a somewhat lame "state machine" of an async request as it moves
     * through various layers of callback completion. The flow is:
     * (OPEN) -> (STARTED) -> (COMPLETED)
     * Any attempt to deviate from this is handled gracefully and not allowed internally. This ensures that
     * we don't complete an async context twice, for example.
     */
    private static final class AsyncContextState {

        private static final int OPEN = 0;
        private static final int STARTED = 1;
        private static final int COMPLETED = 2;

        private final AtomicInteger state_;

        public AsyncContextState() {
            state_ = new AtomicInteger(OPEN);
        }

        public boolean start() {
            return state_.compareAndSet(OPEN, STARTED);
        }

        public boolean complete() {
            return state_.compareAndSet(STARTED, COMPLETED);
        }

    }

    private abstract class AbstractAsyncCompletingCallbackWrapper {

        public abstract void doit() throws Exception;

        public void cant() {
            // No-op
        }

        @SuppressWarnings({"PMD.UseTryWithResources"})
        public final void start() throws Exception {
            // Only attempt to start timeout error handling if this context hasn't been "started" before.
            // If it was already started by another means, then there's nothing we can do.
            if (state_.start()) {
                try {
                    doit();
                } finally {
                    completeQuietly(ctx_.getAsyncContext());
                    ctx_.close();
                    state_.complete();
                }
            } else {
                cant();
            }
        }

        public final void startAndSwallow(
                final String message) {
            try {
                start();
            } catch (final Exception e) {
                LOG.warn(message, e);
            }
        }

        private void completeQuietly(
                final AsyncContext context) {
            try {
                context.complete();
            } catch (final Exception e) {
                LOG.debug("Exception occurred while completing async context.", e);
            }
        }
    }

}
