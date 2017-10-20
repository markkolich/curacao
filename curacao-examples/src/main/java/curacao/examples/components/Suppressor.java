/*
 * Copyright (c) 2017 Mark S. Kolich
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

package curacao.examples.components;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.examples.components.suppress.MockSuppressedComponent;
import curacao.examples.components.suppress.Suppressed;
import curacao.exceptions.CuracaoException;

import javax.annotation.Nonnull;

@Component
public final class Suppressor {

    @Injectable
    public Suppressor(@Nonnull final Suppressed suppressed) {
        // This component expects a mock instance of Suppressed
        if (! (suppressed instanceof MockSuppressedComponent)) {
            throw new CuracaoException("Suppressed injectable must be an instance of: " +
                MockSuppressedComponent.class.getSimpleName());
        }
    }

}
