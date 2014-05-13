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

package com.kolich.curacao.handlers.requests.mappers.types;

import com.kolich.curacao.handlers.requests.CuracaoRequestContext;
import com.kolich.curacao.handlers.requests.mappers.ControllerMethodArgumentMapper;

import javax.annotation.Nullable;
import javax.servlet.ServletInputStream;
import java.lang.annotation.Annotation;

public final class ServletInputStreamMapper
	extends ControllerMethodArgumentMapper<ServletInputStream> {

	@Override
	public final ServletInputStream resolve(@Nullable final Annotation annotation,
                                            final CuracaoRequestContext context) throws Exception {
		// This felt dangerous, but as it turns out, when the request
		// context is completed, the Servlet spec states that the
		// container must forcibly close the input stream and output
		// streams.  If the container does the right thing, this will
		// ~not~ cause leaks.
		return context.request_.getInputStream();
	}

}
