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

package com.kolich.curacao.entities.mediatype;

import java.io.ByteArrayInputStream;

import com.google.common.net.MediaType;

public abstract class AbstractBinaryContentTypeCuracaoEntity
	extends InputStreamCuracaoEntity {
	
	public AbstractBinaryContentTypeCuracaoEntity(final int statusCode,
		final String contentType, final byte[] data) {
		super(statusCode, contentType,
			// Sigh, super() has to be the first line/call in the constructor
			// but I want to assert that the incoming data array is not null
			// before calling super(). This hacky ternary operator trick seems
			// likes the only way to accomplish that without fully reevaluating
			// the type hierarchy of this library.
			(data == null) ? null : new ByteArrayInputStream(data));
	}
	
	public AbstractBinaryContentTypeCuracaoEntity(final int statusCode,
		final MediaType mediaType, final byte[] data) {
		this(statusCode,
			// Only pass the converted media type (content type) to the parent
			// class if the incoming media type was non-null.
			(mediaType == null) ? null : mediaType.toString(),
			data);
	}

}
