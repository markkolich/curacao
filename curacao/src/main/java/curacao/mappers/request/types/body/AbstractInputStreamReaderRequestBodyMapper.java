/*
 * Copyright (c) 2023 Mark S. Kolich
 * https://mark.koli.ch
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

package curacao.mappers.request.types.body;

import curacao.annotations.parameters.RequestBody;
import curacao.context.CuracaoContext;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public abstract class AbstractInputStreamReaderRequestBodyMapper<T>
        extends AbstractMemoryBufferingRequestBodyMapper<T> {

    @Override
    public final T resolveWithBody(
            final RequestBody annotation,
            final CuracaoContext ctx,
            final byte[] body) throws Exception {
        final Charset requestEncoding = getRequestEncoding(ctx);
        try (InputStreamReader reader =
                new InputStreamReader(new ByteArrayInputStream(body), requestEncoding)) {
            return resolveWithReader(reader);
        }
    }

    public abstract T resolveWithReader(
            final InputStreamReader reader) throws Exception;

}
