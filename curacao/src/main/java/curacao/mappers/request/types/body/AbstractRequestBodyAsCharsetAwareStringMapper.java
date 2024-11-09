/*
 * Copyright (c) 2024 Mark S. Kolich
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
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;

public abstract class AbstractRequestBodyAsCharsetAwareStringMapper<T>
        extends AbstractMemoryBufferingRequestBodyMapper<T> {

    @Override
    public final T resolveWithBody(
            final RequestBody annotation,
            final CuracaoContext context,
            final byte[] body) throws Exception {
        // Convert the byte[] array from the request body into a String
        // using the derived character encoding.
        final Charset encoding = getRequestEncoding(context);
        return resolveWithStringAndEncoding(annotation,
                // The encoding String itself.
                StringUtils.toEncodedString(body, encoding),
                // The encoding of the String.
                encoding);
    }

    public abstract T resolveWithStringAndEncoding(
            final RequestBody annotation,
            final String s,
            final Charset encoding) throws Exception;

}
