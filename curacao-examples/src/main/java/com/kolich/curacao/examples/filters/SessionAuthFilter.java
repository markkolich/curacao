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

package com.kolich.curacao.examples.filters;

import com.kolich.curacao.annotations.Injectable;
import com.kolich.curacao.examples.components.SessionCache;
import com.kolich.curacao.examples.entities.SessionObject;
import com.kolich.curacao.examples.exceptions.InvalidOrMissingSessionException;
import com.kolich.curacao.handlers.requests.filters.CuracaoRequestFilter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.examples.components.SessionCacheImpl.SESSION_COOKIE_NAME;

public final class SessionAuthFilter implements CuracaoRequestFilter {

    private final SessionCache cache_;

    @Injectable
    public SessionAuthFilter(final SessionCache cache) {
        cache_ = checkNotNull(cache, "Session cache cannot be null.");
    }
		
	@Override
	public final void filter(final HttpServletRequest request) throws Exception {
        SessionObject session = null;
        final Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(final Cookie cookie : cookies) {
                if(SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    if((session = (SessionObject)cache_.getSession(
                        cookie.getValue())) != null) {
                        break;
                    }
                }
            }
        }
        if(session == null) {
            throw new InvalidOrMissingSessionException("No session found, " +
                "user not authenticated.");
        }
	}

}
