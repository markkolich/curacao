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

package com.kolich.curacao.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kolich.curacao.entities.AppendableCuracaoEntity;

import javax.annotation.Nonnull;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public abstract class JacksonAppendableCuracaoEntity
	extends AppendableCuracaoEntity {
	
	private static final String JSON_UTF_8_TYPE = JSON_UTF_8.toString();

    private final transient ObjectMapper mapper_;

	public JacksonAppendableCuracaoEntity(@Nonnull final ObjectMapper mapper) {
		mapper_ = checkNotNull(mapper, "The Jackson object mapper " +
            "instance cannot be null.");
	}
	
	@Override
	public final void toWriter(final Writer writer) throws Exception {
		mapper_.writeValue(writer, this);
	}

    /**
     * Default, returns 200 OK.  Extending classes should override this method
     * if they wish to return some other status code with the entity when
     * rendered.
     */
	@Override
	public int getStatus() {
		return SC_OK;
	}

    /**
     * This entity always return a MIME Content-Type of application/json.
     * As such, this method is final and cannot be overridden.
     */
	@Override
	public final String getContentType() {
		return JSON_UTF_8_TYPE;
	}

}
