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

package curacao.examples.components;

import curacao.annotations.Component;
import curacao.annotations.Injectable;

/**
 * This class exists to make sure that Curacao can tell the difference
 * between "curacao.examples.components.UserAuthenticator" and
 * this class "curacao.examples.components.UserAuthenticatorConfig".
 * See https://github.com/markkolich/curacao/issues/8
 */
@Component
public final class UserAuthenticatorConfig {

    private final GsonComponent gson_;
    private final SessionCache sessionCache_;

    @Injectable
    public UserAuthenticatorConfig(
            final GsonComponent gson,
            final SessionCache sessionCache) {
        gson_ = gson;
        sessionCache_ = sessionCache;
    }

    public GsonComponent getGson() {
        return gson_;
    }

    public SessionCache getSesssionCache() {
        return sessionCache_;
    }

}
