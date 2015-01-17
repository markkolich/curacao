/**
 * Copyright (c) 2015 Mark S. Kolich
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
import com.kolich.curacao.mappers.request.CuracaoContext;
import com.kolich.curacao.mappers.request.ControllerArgumentMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

import static com.google.common.net.HttpHeaders.USER_AGENT;

@ControllerArgumentTypeMapper(ReverseUserAgent.class)
public final class ReverseUserAgentArgumentMapper
	extends ControllerArgumentMapper<ReverseUserAgent> {

	@Override
	public final ReverseUserAgent resolve(@Nullable final Annotation annotation,
                                          @Nonnull final CuracaoContext ctx) {
		final String ua = ctx.request_.getHeader(USER_AGENT);
		return (ua != null) ?
			new ReverseUserAgent(new StringBuilder(ua).reverse().toString()) :
			null;
	}
	
}
