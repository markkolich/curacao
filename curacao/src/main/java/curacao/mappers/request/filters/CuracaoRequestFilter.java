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

package curacao.mappers.request.filters;

import curacao.CuracaoContext;

import javax.annotation.Nonnull;

/**
 * A request filter defines a class containing a method that will be called as a "pre-processing" event before
 * an underlying controller class method is invoked.  Filters can accept the request, do nothing and simply attaching
 * attributes for consumption by the controller method once invoked.  Or, they can reject the request by
 * throwing an exception.
 */
public interface CuracaoRequestFilter {

    /**
     * Called before the underlying controller method invokable is invoked. This gives the filter a chance to either
     * reject or accept the request. Note that the filter implementation is free to throw any exceptions
     * as need, given these will be gracefully caught and handled by the caller. The filter is free to attach and
     * attributes to the request as needed which can then be accessed by the controller method invokable.
     *
     * @param context The {@link curacao.CuracaoContext} attached to the request.
     * @throws Exception in the event of an error, stops processing and will ask the caller to handle the exception.
     */
	void filter(@Nonnull final CuracaoContext context) throws Exception;

}
