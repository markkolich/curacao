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

import static java.net.URLDecoder.decode;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.kolich.curacao.annotations.parameters.RequestBody;

public final class EncodedPostBodyMultiMapMapper
	extends MemoryBufferingRequestBodyMapper<Multimap<String,String>> {
	
	@Override
	public final Multimap<String,String> resolveSafely(final RequestBody annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final HttpServletResponse response, final byte[] body)
		throws Exception {
		final String charset = getRequestCharset(request);
		final String decoded = decode(StringUtils.toString(body, charset),
			charset);
		//return parse(decoded, charset);
		return null;
	}
	
	private static final Multimap<String,String> convert(final HttpServletRequest request) {
		final Multimap<String,String> map = LinkedHashMultimap.create();
		
		return map;
	}
	
}
