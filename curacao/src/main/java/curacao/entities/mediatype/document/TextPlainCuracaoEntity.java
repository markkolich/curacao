/*
 * Copyright (c) 2017 Mark S. Kolich
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

package curacao.entities.mediatype.document;

import curacao.entities.mediatype.AbstractContentTypeCuracaoEntity;

import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;

public final class TextPlainCuracaoEntity extends AbstractContentTypeCuracaoEntity {
    
    private final String text_;
        
    public TextPlainCuracaoEntity(final int statusCode,
                                  final String text) {
        super(statusCode, PLAIN_TEXT_UTF_8);
        text_ = checkNotNull(text, "The text to render/return cannot be null.");
    }
    
    public TextPlainCuracaoEntity(final String text) {
        this(SC_OK, text);
    }

    @Override
    public final void write(final OutputStream os) throws Exception {
        os.write(getBytesUtf8(text_));
    }

}
