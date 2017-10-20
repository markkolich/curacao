/**
 * Copyright (c) 2016 Mark S. Kolich
 * http://mark.koli.ch
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

import curacao.CuracaoContext;
import curacao.exceptions.async.AsyncContextErrorException;
import curacao.exceptions.async.AsyncContextTimeoutException;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static curacao.CuracaoConfigLoader.getAsyncContextTimeoutMs;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractContextCompletingCallbackHandler extends AbstractFutureCallbackHandler {
	
	private static final Logger log = getLogger(AbstractContextCompletingCallbackHandler.class);
	
	private static final long requestTimeoutMs = getAsyncContextTimeoutMs();

    private static final String asyncErrorMessage = "AsyncContext `error` occurred, " +
		"additionally failed to handle error response.";
    private static final String asyncTimeoutMessage = "AsyncContext `timeout` occurred, " +
		"additionally failed to handle error response.";
	
	/**
	 * An internal class used to model a somewhat lame "state machine" of an async request as it moves
	 * through various layers of callback completion.  The flow is:
	 *   (OPEN) -> (STARTED) -> (COMPLETED)
	 * Any attempt to deviate from this is handled gracefully and not allowed internally.  This ensures that
	 * we don't complete an async context twice, for example.
	 */
	private static final class AsyncContextState {
		private static final int OPEN = 0, STARTED = 1, COMPLETED = 2;
		private final AtomicInteger state_;
		public AsyncContextState() {
			state_ = new AtomicInteger(OPEN);
		}
		public final boolean start() {
			return state_.compareAndSet(OPEN, STARTED);
		}
		public final boolean complete() {
			return state_.compareAndSet(STARTED, COMPLETED);
		}
	}
	
	private abstract class AsyncCompletingCallbackWrapper {
		public abstract void doit() throws Exception;
		public void cant() { } // NOOP
		public final void start() throws Exception {
			// Only attempt to start timeout error handling if this context hasn't been "started" before.
			// If it was already started by another means, then there's nothing we can do.
			if (state_.start()) {
				try {
					doit();
				} finally {
					completeQuietly(ctx_.asyncCtx_);
					state_.complete();
				}
			} else {
				cant();
			}
		}
		public final void startAndSwallow(final String message) {
			try {
				start();
			} catch (Exception e) {
                log.warn(message, e);
            }
		}
		private final void completeQuietly(final AsyncContext context) {
			try {
				context.complete();
			} catch (Exception e) {
				log.debug("Exception occurred while completing async context.", e);
			}
		}
	}
	
	private final AsyncContextState state_;
	
	public AbstractContextCompletingCallbackHandler(@Nonnull final CuracaoContext ctx) {
		super(ctx);
        // Pull off the async context attached to this Curacao context.
        final AsyncContext aCtx = ctx_.asyncCtx_;
        // Bind a fresh async listener to the async context.
		aCtx.addListener(getAsyncListener());
		// Set the async context request timeout as set in the config.
		// Note, a value of 0L means "never timeout".
		aCtx.setTimeout(requestTimeoutMs);
        // Local properties.
		state_ = new AsyncContextState();
	}
	
	private final AsyncListener getAsyncListener() {
		// Note that when the Servlet container invokes one of these methods in the AsyncListener, it's invoked
		// in the context of a thread owned and managed by the container.  That is, it's executed "on a thread that
		// belongs to the container" and not managed by Curacao.
		return new AsyncListener() {
			@Override
			public void onStartAsync(final AsyncEvent event) throws IOException { }
			@Override
			public void onComplete(final AsyncEvent event) throws IOException { }
			@Override
			public void onError(final AsyncEvent event) throws IOException {
				new AsyncCompletingCallbackWrapper() {
					@Override
					public void doit() throws Exception {
                        Throwable cause = event.getThrowable();
                        if (cause == null) {
                            cause = new AsyncContextErrorException(ctx_.comment_);
                        }
                        renderFailure(cause);
					}
				}.startAndSwallow(asyncErrorMessage);
			}
			@Override
			public void onTimeout(final AsyncEvent event) throws IOException {
				new AsyncCompletingCallbackWrapper() {
					@Override
					public void doit() throws Exception {
                        Throwable cause = event.getThrowable();
                        if (cause == null) {
                            cause = new AsyncContextTimeoutException("Async context not completed within " +
									requestTimeoutMs + "-ms timeout: " + ctx_.comment_);
                        }
                        renderFailure(cause);
					}
				}.startAndSwallow(asyncTimeoutMessage);
			}
		};
	}

	@Override
	public final void successAndComplete(@Nonnull final Object result) throws Exception {
		new AsyncCompletingCallbackWrapper() {
			@Override
			public void doit() throws Exception {
				renderSuccess(result);
			}
			@Override
			public void cant() {
				// You'd get here if the async context timed out, and the AsyncListener.onTimeout() method was called
				// to handle the timeout event (and render an error response). But, at some point in the future a slow
				// controller finishes and tries to complete the context again after data has already been written out
				// and the context completed.
				log.warn("On success and complete: attempted to start & render response after context " +
					"state was already `started`; ignoring!");
			}
		}.start();
	}

	@Override
	public final void failureAndComplete(@Nonnull final Throwable t) throws Exception {
		new AsyncCompletingCallbackWrapper() {
			@Override
			public void doit() throws Exception {
				renderFailure(t);
			}
			@Override
			public void cant() {
				// You'd get here if the async context timed out, and the AsyncListener.onTimeout() method was called
				// to handle the timeout event (and render an error response). But, at some point in the future a slow
				// controller finishes and tries to complete the context again after data has already been written out
				// and the context completed.
				log.warn("On failure and complete: attempted to start & render response after context " +
					"state was already `started`; ignoring!");
			}
		}.start();
	}
	
	public abstract void renderSuccess(@Nonnull final Object result)
		throws Exception;
	
	public abstract void renderFailure(@Nonnull final Throwable t)
		throws Exception;

}
