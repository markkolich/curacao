/*
 * Copyright (c) 2026 Mark S. Kolich
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

package curacao.exceptions.reflection;

import curacao.exceptions.CuracaoException;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * A mistake in how the application declares its components, controllers, filters or mappers, found at startup.
 * It must fail startup: a route or mapper that couldn't be built because of one is never just logged and
 * skipped.
 */
public abstract class AbstractConfigurationException extends CuracaoException {

    protected AbstractConfigurationException(
            final String message) {
        super(message);
    }

    /**
     * Throws the configuration exception in the throwable's cause chain, if there's one; a route or mapper is
     * often built behind a cache or a wrapping exception.
     */
    public static void rethrowIfPresent(
            final Throwable throwable) {
        final int index = ExceptionUtils.indexOfType(throwable, AbstractConfigurationException.class);
        if (index >= 0) {
            throw (AbstractConfigurationException) ExceptionUtils.getThrowableList(throwable).get(index);
        }
    }

}
