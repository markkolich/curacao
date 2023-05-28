/*
 * Copyright (c) 2023 Mark S. Kolich
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

package curacao.examples.filters;

import curacao.annotations.Injectable;
import curacao.context.CuracaoContext;
import curacao.core.servlet.HttpCookie;
import curacao.examples.components.SessionCache;
import curacao.examples.entities.SessionObject;
import curacao.examples.exceptions.InvalidOrMissingSessionException;
import curacao.mappers.request.filters.CuracaoRequestFilter;

import javax.annotation.Nonnull;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.examples.components.SessionCacheImpl.SESSION_COOKIE_NAME;

public final class SessionAuthFilter implements CuracaoRequestFilter {

    private final SessionCache cache_;

    @Injectable
    public SessionAuthFilter(
            @Nonnull final SessionCache cache) {
        cache_ = checkNotNull(cache, "Session cache cannot be null.");
    }

    @Override
    public void filter(
            @Nonnull final CuracaoContext ctx) {
        SessionObject session = null;
        final List<HttpCookie> cookies = ctx.getRequest().getCookies();
        if (cookies != null) {
            for (final HttpCookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    session = (SessionObject) cache_.getSession(cookie.getValue());
                    if (session != null) {
                        break;
                    }
                }
            }
        }
        if (session == null) {
            throw new InvalidOrMissingSessionException("No session found, user not authenticated.");
        }
    }

}
