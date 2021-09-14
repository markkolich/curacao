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

package curacao;

import com.google.common.collect.ImmutableMap;
import org.slf4j.MDC;

import javax.annotation.Nonnull;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Copies the MDC (mapped diagnostic context) from the invoking/parent thread into a thread local accessible
 * by the child thread (the wrapped runnable). This preserves the MDC across threads.
 */
public final class CuracaoRunnable implements Runnable {

    private static final Map<String, String> EMPTY_IMMUTABLE_MAP = ImmutableMap.of();

    private final Runnable wrapped_;

    private final Map<String, String> mdcContextMap_;

    public CuracaoRunnable(
            @Nonnull final Runnable wrapped) {
        wrapped_ = checkNotNull(wrapped, "Wrapped runnable cannot be null.");
        // Grab a copy of the thread local MDC (Mapped Diagnostic Context). This is used to preserve the
        // diagnostic context across threads as Curacao requests+responses are handled by multiple threads in
        // a pool.
        Map<String, String> cachedContext = MDC.getCopyOfContextMap();
        // Annoyingly, getCopyOfContextMap() returns null if no map was set. It would be *nice* if it just
        // returned an empty map to avoid NPE's downstream, but whatever.
        if (cachedContext == null) {
            cachedContext = EMPTY_IMMUTABLE_MAP;
        }
        mdcContextMap_ = cachedContext;
    }

    @Override
    public void run() {
        try {
            // Set the current thread's MDC to our parent "copy", and invoke the wrapped runnable.
            MDC.setContextMap(mdcContextMap_);
            wrapped_.run();
        } finally {
            // Detach/clear the MDC from the child's thread local space.
            MDC.clear();
        }
    }

}
