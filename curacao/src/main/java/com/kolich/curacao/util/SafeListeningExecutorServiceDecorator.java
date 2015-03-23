/**
 * Copyright (c) 2015 Mark S. Kolich
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

package com.kolich.curacao.util;

import com.google.common.util.concurrent.AbstractListeningExecutorService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.kolich.curacao.CuracaoRunnable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is a custom implementation of a {@link ListeningExecutorService}
 * so we can control the execution of new runnables.  The Google default
 * implementation of their internal ListeningDecorator blindly submits a
 * runnable for execution even if the delegate executor service is shutdown.
 * This results in a total spew of excessive {@link RejectedExecutionException}'s
 * which can be totally prevented.
 */
public final class SafeListeningExecutorServiceDecorator
    extends AbstractListeningExecutorService {

    private final ExecutorService delegate_;

    public SafeListeningExecutorServiceDecorator(@Nonnull final ExecutorService delegate) {
        delegate_ = checkNotNull(delegate, "Executor service cannot be null.");
    }

    @Override
    public boolean awaitTermination(final long timeout,
                                    final TimeUnit unit) throws InterruptedException {
        return delegate_.awaitTermination(timeout, unit);
    }

    @Override
    public boolean isShutdown() {
        return delegate_.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate_.isTerminated();
    }

    @Override
    public void shutdown() {
        delegate_.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate_.shutdownNow();
    }

    @Override
    public void execute(final Runnable command) {
        // DO NOT submit the runnable to the delegate if it's shutdown/stopped.
        if (!delegate_.isShutdown()) {
            delegate_.execute(new CuracaoRunnable(command));
        }
    }

}
