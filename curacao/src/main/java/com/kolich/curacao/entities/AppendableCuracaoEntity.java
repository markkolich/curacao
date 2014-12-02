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

package com.kolich.curacao.entities;

import javax.annotation.Nonnull;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AppendableCuracaoEntity implements CuracaoEntity {

    private static final String UTF_8_CHARSET = UTF_8.toString();
	
	// Note, this field is marked transient intentionally since it
	// should be omitted during any serialization or deserialization
	// events.
	private final transient String charsetName_;
	
	public AppendableCuracaoEntity(@Nonnull final String charsetName) {
		charsetName_ = checkNotNull(charsetName, "Charset name cannot " +
			"be null.");
	}
	
	public AppendableCuracaoEntity() {
		this(UTF_8_CHARSET);
	}

	@Override
	public final void write(final OutputStream os) throws Exception {
        // <https://github.com/markkolich/curacao/issues/13>
        // NOTE: Although the paranoid reader might look at this and say
        // WTF why are you not closing the writer?  The even more astute reader
        // will recognize that if you close the writer, you're also going to
        // close the wrapped OutputStream as a result.  This leads to all sorts
        // awesome bugs, given if the Servlet container expects to do something
        // with this OutputStream, it can't because it's been closed.  The
        // code here was originally try/finally closing the writer, but through
        // a bug we realized that was the wrong thing to do and the writer
        // should definitely not be closed.
		final OutputStreamWriter w = new OutputStreamWriter(os, charsetName_);
        toWriter(w);
        w.flush(); // Important!
	}

    /**
     * Called when this entity should write itself out to the provided
     * {@link Writer} writer.  Note that {@link Writer}'s are character
     * encoding aware, and so this class honors that by passing a functional
     * {@link OutputStreamWriter} already initialized with the right character
     * encoding into this method.
     *
     * The implementor (extending class) should not close the provided
     * {@link Writer} writer.  Closing the passed writer will close the
     * underlying {@link OutputStream} too, as provided by the Servlet
     * container. Closing this output stream prematurely creates a number
     * of unexpected and problematic corner cases in the Servlet container
     * which leads to nasty bugs.
     */
	public abstract void toWriter(final Writer writer) throws Exception;

}
