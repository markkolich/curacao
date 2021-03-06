/*
 * Copyright (c) 2019 Mark S. Kolich
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

package curacao.mappers.response.types.resources;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;

import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;

public final class NotModifiedResponseEntity extends UnmodifiableCacheableEntity {

    public NotModifiedResponseEntity(@Nonnull final HttpServletResponse response,
                                     @Nonnull final File file,
                                     @Nonnull final String eTag) {
        super(response, file, eTag);
    }

    @Override
    public final int getStatus() {
        return SC_NOT_MODIFIED;
    }

    @Override
    public final String getContentType() {
        return null;
    }

    @Override
    public final void writeAfterHeaders(final OutputStream os) throws Exception {
        // Nothing, intentional.
    }

}
