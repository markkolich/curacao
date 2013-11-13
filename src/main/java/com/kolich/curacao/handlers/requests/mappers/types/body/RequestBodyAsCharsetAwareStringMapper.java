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

package com.kolich.curacao.handlers.requests.mappers.types.body;

import static com.google.common.base.Charsets.ISO_8859_1;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

public final class RequestBodyAsCharsetAwareStringMapper
	extends MemoryBufferingRequestBodyMapper<String> {

	@Override
	public final String resolveSafe(final Annotation annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final HttpServletResponse response, final byte[] body)
		throws Exception {
		String result = null, encoding = request.getCharacterEncoding();			
		if(encoding == null) {
			// HTTP/1.1 says that the default charset is ISO-8859-1 if
			// not specified.
			encoding = ISO_8859_1.toString();
		}
		// Convert the byte[] array from the request body into a String
		// using the derived character encoding.
		result = StringUtils.toString(body, encoding);
		return result;
	}

}
