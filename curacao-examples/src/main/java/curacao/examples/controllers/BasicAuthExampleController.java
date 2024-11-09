/*
 * Copyright (c) 2024 Mark S. Kolich
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

package curacao.examples.controllers;

import curacao.annotations.Controller;
import curacao.annotations.RequestMapping;
import curacao.core.servlet.HttpRequest;
import curacao.core.servlet.HttpResponse;
import curacao.entities.CuracaoEntity;
import curacao.entities.mediatype.document.TextPlainUtf8CuracaoEntity;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static curacao.core.servlet.HttpStatus.SC_UNAUTHORIZED;

@Controller
public final class BasicAuthExampleController {

    private static final String REALM = "BasicAuthExampleController";

    @RequestMapping("^/api/secure$")
    public CuracaoEntity basicAuth(
            final HttpRequest request,
            final HttpResponse response) {
        return new AbstractBasicAuthClosure<CuracaoEntity>(request, response, REALM) {
            @Override
            public CuracaoEntity authorized() {
                return new TextPlainUtf8CuracaoEntity("It worked!");
            }

            @Override
            public CuracaoEntity unauthorized() {
                return new TextPlainUtf8CuracaoEntity(SC_UNAUTHORIZED,
                    "Oops, unauthorized.");
            }
        }.execute();
    }

    private abstract static class AbstractBasicAuthClosure<T extends CuracaoEntity> {

        private final HttpRequest request_;
        private final HttpResponse response_;
        private final String realm_;

        public AbstractBasicAuthClosure(
                final HttpRequest request,
                final HttpResponse response,
                final String realm) {
            request_ = request;
            response_ = response;
            realm_ = realm;
        }

        public abstract T authorized();

        public abstract T unauthorized();

        public final T execute() {
            final String authorization = request_.getHeader(AUTHORIZATION);
            if (authorization == null) {
                response_.addHeader(WWW_AUTHENTICATE, String.format("Basic realm=\"%s\"", realm_));
                return unauthorized();
            } else {
                return authorized();
            }
        }

    }

}
