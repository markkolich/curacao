/*
 * Copyright (c) 2017 Mark S. Kolich
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

package curacao.examples.handlers;

import com.google.common.util.concurrent.FutureCallback;
import curacao.context.CuracaoContext;
import curacao.handlers.ReturnTypeMapperCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class TimerAwareFutureCallback implements FutureCallback<Object> {

    private static final Logger log = LoggerFactory.getLogger(TimerAwareFutureCallback.class);

    private final CuracaoContext ctx_;
    private final ReturnTypeMapperCallbackHandler delegate_;

    public TimerAwareFutureCallback(@Nonnull final CuracaoContext ctx) {
        ctx_ = checkNotNull(ctx, "Curacao context cannot be null");
        delegate_ = new ReturnTypeMapperCallbackHandler(ctx_);
    }

    @Override
    public final void onSuccess(@Nullable final Object result) {
        try {
            delegate_.onSuccess(result);
        } finally {
            log.info("[Success] Request took {}-ms.", (System.currentTimeMillis()-ctx_.getCreationTime()));
        }
    }

    @Override
    public final void onFailure(final Throwable t) {
        try {
            delegate_.onFailure(t);
        } finally {
            log.info("[Failure] Request took {}-ms.", (System.currentTimeMillis()-ctx_.getCreationTime()));
        }
    }

}
