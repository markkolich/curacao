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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import curacao.annotations.Component;
import curacao.annotations.Injectable;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class GsonComponent {

    @Injectable
    public GsonComponent(
            final SessionCache cache,
            final InnerClass inner) {
        checkNotNull(cache, "Session cache cannot be null.");
        checkNotNull(inner, "Inner class cannot be null.");
    }

    public Gson getGsonInstance() {
        return new GsonBuilder().serializeNulls().create();
    }

    /**
     * This is here to validate and demonstrate that "inner static" classes can be
     * components too.
     */
    @Component
    public static final class InnerClass {

        @Injectable
        public InnerClass(
                final JacksonComponent jackson) {
            checkNotNull(jackson, "Jackson component cannot be null.");
        }

    }

}
