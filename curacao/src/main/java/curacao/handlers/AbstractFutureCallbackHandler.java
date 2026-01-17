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

package curacao.handlers;

import com.google.common.util.concurrent.FutureCallback;
import curacao.context.CuracaoContext;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractFutureCallbackHandler implements FutureCallback<Object> {

    private static final Logger LOG = getLogger(AbstractFutureCallbackHandler.class);

    protected final CuracaoContext ctx_;

    public AbstractFutureCallbackHandler(
            @Nonnull final CuracaoContext ctx) {
        ctx_ = checkNotNull(ctx, "Curacao context cannot be null.");
    }

    @Override
    public final void onSuccess(
            @Nullable final Object result) {
        try {
            // Only attempt to lookup a response "handler" for the resulting object if the invoked controller
            // method returned an actual non-null value. In the case where the controller returned null,
            // the contract is that the controller is then responsible for handling the entire response, including
            // completing the async context.
            if (result != null) {
                successAndComplete(result);
            }
        } catch (final Throwable t) {
            // There's very little that could be done at this point to "salvage" the response going back to the
            // client. Even if we did try and reset the HTTP response status code to indicate error, that may fail
            // given the renderer could have already set the status and may have sent some data down to the client.
            // Based on the nature of HTTP, if an HTTP response code was sent by the Servlet container followed by
            // some data, it's impossible for the renderer (or even this library) to go back and reset/change the
            // response code with the client once that status has already been sent.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed miserably to render 'success' response; abandoning!", t);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("Failed miserably to render 'success' response; abandoning! (type={}, message={})",
                        t.getClass().getCanonicalName(), t.getMessage());
            }
        }
    }

    @Override
    public final void onFailure(
            @Nonnull final Throwable throwable) {
        try {
            final Throwable cause;
            if (throwable instanceof ExecutionException) {
                cause = (throwable.getCause() != null) ? throwable.getCause() : throwable;
            } else if (throwable instanceof InvocationTargetException) {
                // In reflection land, when a reflection invoked method throws an exception,
                // it's inconveniently wrapped in a InvocationTargetException. So, we
                // have to unwrap the "real" exception from within the passed throwable.
                cause = (throwable.getCause() != null) ? throwable.getCause() : throwable;
            } else {
                cause = throwable;
            }

            failureAndComplete(cause);
        } catch (final Throwable t) {
            // There's very little that could be done at this point to "salvage" the response going back to the
            // client. Even if we did try and reset the HTTP response status code to indicate error, that may fail
            // given the renderer could have already set the status and may have sent some data down to the client.
            // Based on the nature of HTTP, if an HTTP response code was sent by the Servlet container followed by
            // some data, it's impossible for the renderer (or even this library) to go back and reset/change the
            // response code with the client once that status has already been sent.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed miserably to render 'failure' response; abandoning!", t);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("Failed miserably to render 'failure' response; abandoning! (type={}, message={})",
                        t.getClass().getCanonicalName(), t.getMessage());
            }
        }
    }

    public abstract void successAndComplete(
            @Nonnull final Object result) throws Exception;

    public abstract void failureAndComplete(
            @Nonnull final Throwable t) throws Exception;

}
