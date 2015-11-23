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

package curacao.entities;

import javax.annotation.Nonnull;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AppendableCuracaoEntity implements CuracaoEntity {

    private static final String UTF_8_CHARSET = UTF_8.toString();
	
	// Note, this field is marked transient intentionally since it should be omitted during any
	// serialization or deserialization events.
	private final transient String charsetName_;
	
	public AppendableCuracaoEntity(@Nonnull final String charsetName) {
		charsetName_ = checkNotNull(charsetName, "Charset name cannot be null.");
	}
	
	public AppendableCuracaoEntity() {
		this(UTF_8_CHARSET);
	}

	@Override
	public final void write(final OutputStream os) throws Exception {
		try (final OutputStreamWriter w = new OutputStreamWriter(os, charsetName_)) {
            toWriter(w);
		}
	}

    /**
     * Called when this entity should write itself out to the provided {@link Writer} writer.  Note that
	 * {@link Writer}'s are character encoding aware, and so this class honors that by passing a functional
     * {@link OutputStreamWriter} already initialized with the right character encoding into this method.
     */
	public abstract void toWriter(final Writer writer) throws Exception;

}
