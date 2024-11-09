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

package curacao.mappers.request.types;

import com.google.common.primitives.Longs;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.Query;
import curacao.annotations.parameters.convenience.ContentLength;
import curacao.context.CuracaoContext;
import curacao.core.servlet.HttpRequest;
import curacao.exceptions.requests.MissingRequiredParameterException;
import curacao.mappers.request.AbstractControllerArgumentMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

public final class LongArgumentMapper extends AbstractControllerArgumentMapper<Long> {

    @Override
    public Long resolve(
            @Nullable final Annotation annotation,
            @Nonnull final CuracaoContext ctx) throws Exception {
        final HttpRequest request = ctx.getRequest();
        Long result = null;
        if (annotation instanceof ContentLength) {
            // It seems that getContentLengthLong() is only available in Servlet 3.1 containers.
            // If we want this library to also run in Servlet 3.0 environments, then we can't call
            // getContentLengthLong(). Instead, we call the typical getContentLength() and use
            // Long.valueOf() to return that integer value as a Long.
            result = (long) ctx.getRequest().getContentLength();
        } else if (annotation instanceof Query) {
            final Query query = (Query) annotation;
            final String number = request.getParameter(query.value());
            if (number == null && query.required()) {
                throw new MissingRequiredParameterException("Request missing required query parameter: "
                        + query.value());
            } else if (number != null) {
                // Returns null instead of throwing an exception if parsing fails.
                result = Longs.tryParse(number);
            }
        } else if (annotation instanceof Path) {
            final String number =
                    CuracaoContext.Extensions.getPathVariables(ctx).get(((Path) annotation).value());
            if (number != null) {
                // Returns null instead of throwing an exception if parsing fails.
                result = Longs.tryParse(number);
            }
        }
        return result;
    }

}
