/**
 * Copyright (c) 2013 Mark S. Kolich
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
import com.kolich.curacao.annotations.methods.GET;
import com.kolich.curacao.annotations.methods.POST;
import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.examples.components.SessionCache;
import com.kolich.curacao.examples.components.UserAuthenticator;

import javax.servlet.AsyncContext;

@Controller
public final class LoginController {

    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";

    private final SessionCache cache_;
    private final UserAuthenticator authenticator_;

    @Injectable
    public LoginController(final SessionCache cache,
        final UserAuthenticator authenticator) {
        cache_ = cache;
        authenticator_ = authenticator;
    }

    @GET("/api/login")
    public final void showLogin(final AsyncContext context) {
        context.dispatch("/WEB-INF/jsp/login.jsp");
    }
	
	@POST("/api/login")
	public final void login(@RequestBody(USERNAME_FIELD) final String username,
        @RequestBody(PASSWORD_FIELD) final String password,
        final AsyncContext context) {
        if(authenticator_.isValidLogin(username, password)) {
            context.dispatch("/WEB-INF/jsp/demo.jsp");
        } else {
            context.dispatch("/WEB-INF/jsp/login.jsp");
        }
	}

}