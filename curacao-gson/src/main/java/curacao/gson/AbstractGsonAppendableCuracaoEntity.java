/*
 * Copyright (c) 2021 Mark S. Kolich
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

package curacao.gson;

import com.google.gson.Gson;
import curacao.entities.AbstractAppendableCuracaoEntity;

import javax.annotation.Nonnull;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;

public abstract class AbstractGsonAppendableCuracaoEntity extends AbstractAppendableCuracaoEntity {

    private static final int SC_OK = 200;
    private static final String JSON_UTF_8_TYPE = JSON_UTF_8.toString();

    // Is intentionally "transient" to avoid serialization by GSON.
    private final transient Gson gson_;

    public AbstractGsonAppendableCuracaoEntity(
            @Nonnull final Gson gson) {
        gson_ = checkNotNull(gson, "The GSON instance cannot be null.");
    }

    @Override
    public final void toWriter(
            final Writer writer) throws Exception {
        gson_.toJson(this, writer);
    }

    /**
     * Default, returns 200 OK. Extending classes should override this method if they wish to return some
     * other status code with the entity when rendered.
     */
    @Override
    public int getStatus() {
        return SC_OK;
    }

    /**
     * This entity always return a MIME Content-Type of 'application/json'.
     * As such, this method is declared final and cannot be overridden.
     */
    @Override
    public final String getContentType() {
        return JSON_UTF_8_TYPE;
    }

}
