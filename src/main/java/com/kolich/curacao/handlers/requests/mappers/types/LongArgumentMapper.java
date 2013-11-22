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

package com.kolich.curacao.handlers.requests.mappers.types;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kolich.curacao.annotations.parameters.convenience.ContentLength;
import com.kolich.curacao.handlers.requests.mappers.ControllerMethodArgumentMapper;

public final class LongArgumentMapper
	extends ControllerMethodArgumentMapper<Long> {

	@Override
	public final Long resolve(final Annotation annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final HttpServletResponse response) throws Exception {
		Long result = null;
		if(annotation instanceof ContentLength) {
			// It seems that getContentLengthLong() is only available in
			// Servlet 3.1 containers.  If we want this library to also run
			// in Servlet 3.0 environments, then we can't call
			// getContentLengthLong().  Instead, we call the typical
			// getContentLength() and use Long.valueOf() to return that
			// integer value as a Long.
			//result = request.getContentLengthLong(); // Only works in Servlet 3.1
			result = Long.valueOf(request.getContentLength());
		}
		return result;
	}

}
