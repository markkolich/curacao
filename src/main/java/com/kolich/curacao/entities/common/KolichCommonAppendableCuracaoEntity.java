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

package com.kolich.curacao.entities.common;

import static com.kolich.common.DefaultCharacterEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.kolich.common.entities.KolichCommonEntity;
import com.kolich.curacao.entities.CuracaoEntity;

public abstract class KolichCommonAppendableCuracaoEntity
	extends KolichCommonEntity implements CuracaoEntity {
	
	// Note, this field is marked transient intentionally since it
	// should be omitted during any serialization or deserialization
	// events.
	private final transient String charsetName_;
	
	public KolichCommonAppendableCuracaoEntity(
		final String charsetName) {
		charsetName_ = charsetName;
	}
	
	public KolichCommonAppendableCuracaoEntity() {
		this(UTF_8);
	}

	@Override
	public final void write(final OutputStream os) throws Exception {
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(os, charsetName_);
			toWriter(writer);
			writer.flush();
		} finally {
			closeQuietly(writer);
		}
	}
	
	public abstract void toWriter(final Appendable writer) throws Exception;

}
