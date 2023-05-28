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

package curacao.examples.mappers.response;

import curacao.annotations.Mapper;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import curacao.examples.entities.MyCustomObject;
import curacao.mappers.response.AbstractControllerReturnTypeMapper;

import javax.annotation.Nonnull;
import java.io.Writer;

import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static curacao.core.servlet.HttpStatus.SC_OK;

@Mapper
public final class MyCustomObjectReturnMapper
        extends AbstractControllerReturnTypeMapper<MyCustomObject> {

    private static final String PLAIN_TEXT_CONTENT_TYPE = PLAIN_TEXT_UTF_8.toString();

    @Override
    public void render(
            final AsyncContext context,
            final HttpResponse response,
            @Nonnull final MyCustomObject entity) throws Exception {
        response.setStatus(SC_OK);
        response.setContentType(PLAIN_TEXT_CONTENT_TYPE);
        try (Writer w = response.getWriter()) {
            w.write(new StringBuilder(entity.toString()).reverse().toString());
        }
    }

}
