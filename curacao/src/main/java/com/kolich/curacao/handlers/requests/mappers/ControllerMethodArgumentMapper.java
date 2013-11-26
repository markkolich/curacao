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

package com.kolich.curacao.handlers.requests.mappers;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class ControllerMethodArgumentMapper<T> {

    /**
     * Called when the mapper is asked to lookup the argument/parameter
     * from the request for the controller method invocation.  The returned
     * value will be passed into the controller method as an argument when
     * invoked via reflection by this library.
     * @param annotation the first annotation attached to the argument, if any.
     * @param pathVars the path variables map for the request.
     * @param request the underlying {@link HttpServletRequest} itself.
     * @param response the underlying {@link HttpServletResponse} itself.
     * @return an object of type T if the mapper could extract a valid argument
     * from the incoming request.  Should return null if no argument could be
     * discovered or extracted.
     * @throws Exception in the event of an error or exception case.
     */
    @Nullable
	public abstract T resolve(@Nullable final Annotation annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final HttpServletResponse response) throws Exception;

}
