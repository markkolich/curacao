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

package curacao.mappers.response;

import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import curacao.entities.CuracaoEntity;
import curacao.entities.empty.StatusCodeOnlyCuracaoEntity;

import javax.annotation.Nonnull;
import java.io.OutputStream;

public abstract class AbstractControllerReturnTypeMapper<T> {

    @SuppressWarnings("unchecked")
    public final void renderObject(
            final AsyncContext context,
            final HttpResponse response,
            @Nonnull final Object obj) throws Exception {
        // Meh, total shim to coerce the incoming 'obj' of type Object into an object of type T.
        render(context, response, (T) obj);
    }

    public abstract void render(
            final AsyncContext context,
            final HttpResponse response,
            @Nonnull final T entity) throws Exception;

    protected static void renderEntity(
            final HttpResponse response,
            final int statusCode) throws Exception {
        // Renders an empty response body with the right HTTP response (status) code.
        renderEntity(response, new StatusCodeOnlyCuracaoEntity(statusCode));
    }

    protected static void renderEntity(
            final HttpResponse response,
            final CuracaoEntity entity) throws Exception {
        try (OutputStream os = response.getOutputStream()) {
            response.setStatus(entity.getStatus());
            // Only set the Content-Type on the response if the entity has a content type.
            // A null type means no Content-Type.
            final String contentType = entity.getContentType();
            if (contentType != null) {
                response.setContentType(contentType);
            }
            entity.write(os);
        }
    }

}
