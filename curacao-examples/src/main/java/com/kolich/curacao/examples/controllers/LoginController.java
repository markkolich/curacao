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

package com.kolich.curacao.examples.controllers;

import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.Injectable;
import com.kolich.curacao.annotations.methods.RequestMapping;
import com.kolich.curacao.annotations.methods.RequestMapping.RequestMethod;
import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.annotations.parameters.convenience.Cookie;
import com.kolich.curacao.examples.components.SessionCache;
import com.kolich.curacao.examples.components.UserAuthenticator;
import com.kolich.curacao.examples.entities.SessionObject;
import com.kolich.curacao.examples.filters.SessionAuthFilter;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.examples.components.SessionCacheImpl.SESSION_COOKIE_NAME;
import static com.kolich.curacao.examples.components.SessionCacheImpl.getRandomSessionId;

@Controller
public final class LoginController {

    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";

    private final SessionCache cache_;
    private final UserAuthenticator authenticator_;

    @Injectable
    public LoginController(final SessionCache cache,
                           final UserAuthenticator authenticator) {
        cache_ = checkNotNull(cache, "Session cache cannot be null.");
        authenticator_ = checkNotNull(authenticator, "Authenticator cannot " +
            "be null.");
    }

    @RequestMapping("^\\/api\\/login$")
    public final void showLogin(final AsyncContext context) {
        context.dispatch("/WEB-INF/jsp/login.jsp");
    }
	
	@RequestMapping(value="^\\/api\\/login$", methods=RequestMethod.POST)
	public final void login(@RequestBody(USERNAME_FIELD) final String username,
                            @RequestBody(PASSWORD_FIELD) final String password,
                            final HttpServletResponse response,
                            final AsyncContext context) throws Exception {
        if(authenticator_.isValidLogin(username, password)) {
            final String sessionId = getRandomSessionId();
            cache_.setSession(sessionId, new SessionObject(username));
            response.addCookie(new javax.servlet.http.Cookie(
                SESSION_COOKIE_NAME, sessionId));
            response.sendRedirect("home");
            context.complete();
        } else {
            context.dispatch("/WEB-INF/jsp/login.jsp");
        }
	}

    @RequestMapping(value="^\\/api\\/home$", filter=SessionAuthFilter.class)
    public final void showSecuredHome(final AsyncContext context) {
        context.dispatch("/WEB-INF/jsp/home.jsp");
    }

    @RequestMapping(value="^\\/api\\/logout$", filter=SessionAuthFilter.class)
    public final void doLogout(@Cookie(SESSION_COOKIE_NAME) final String sessionId,
                               final HttpServletResponse response,
                               final AsyncContext context)
        throws Exception {
        cache_.removeSession(sessionId);
        final javax.servlet.http.Cookie unset = new javax.servlet.http.Cookie(
            SESSION_COOKIE_NAME, sessionId);
        unset.setMaxAge(0);
        response.addCookie(unset);
        response.sendRedirect("login");
        context.complete();
    }

}
