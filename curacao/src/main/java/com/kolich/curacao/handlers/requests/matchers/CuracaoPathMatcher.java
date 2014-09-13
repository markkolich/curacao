/**
 * Copyright (c) 2014 Mark S. Kolich
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

package com.kolich.curacao.handlers.requests.matchers;

import com.kolich.curacao.handlers.requests.CuracaoRequestContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public interface CuracaoPathMatcher {

    /**
     * Given the context, matcher key, and complete request URI, attempt
     * to match the provided path to the given key.  If there is a match,
     * this method should extract and return a {@link Map} that maps each
     * named "capture group" in the key to its value from the path.  If there
     * is no match, this method should return null.
     * Note that this method very likely returns, in many cases, an immutable
     * map that does not support operations such as put() or clear().  The
     * consumer should not modify the resulting map, and be prepared for
     * unexpected failures (e.g., UnsupportedOperationException's) if they do.
     * @param context the mutable {@link CuracaoRequestContext} object of this request
     * @param key the routing key key to which a matcher can use to match
     *            the path on the incoming request
     * @param path the full request URI, without the application context (if any)
     * @return a {@link Map} which maps each named capture group to its value,
     * or null if no match was found (the provided key did not match the
     * path)
     * @throws Exception if anything went wrong
     */
    @Nullable
	public Map<String,String> match(@Nonnull final CuracaoRequestContext context,
                                    @Nonnull final String key,
                                    @Nonnull final String path) throws Exception;

}
