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

package com.kolich.curacao.examples.mappers;

import com.kolich.curacao.annotations.mappers.ControllerArgumentTypeMapper;
import com.kolich.curacao.examples.entities.ReverseUserAgent;
import com.kolich.curacao.handlers.requests.CuracaoRequestContext;
import com.kolich.curacao.handlers.requests.mappers.ControllerMethodArgumentMapper;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

import static com.google.common.net.HttpHeaders.USER_AGENT;

@ControllerArgumentTypeMapper(ReverseUserAgent.class)
public final class ReverseUserAgentArgumentMapper
	extends ControllerMethodArgumentMapper<ReverseUserAgent> {

	@Override
	public final ReverseUserAgent resolve(@Nullable final Annotation annotation,
                                          final CuracaoRequestContext context) {
		final String ua = context.request_.getHeader(USER_AGENT);
		return (ua != null) ?
			new ReverseUserAgent(new StringBuilder(ua).reverse().toString()) :
			null;
	}
	
}
