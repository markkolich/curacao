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

import com.google.common.collect.ImmutableSet;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.Query;
import curacao.context.CuracaoContext;
import curacao.core.servlet.HttpRequest;
import curacao.exceptions.requests.MissingRequiredParameterException;
import curacao.mappers.request.AbstractControllerArgumentMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Set;

public final class BooleanArgumentMapper extends AbstractControllerArgumentMapper<Boolean> {

    private static final Set<String> TRUE_VALUES = ImmutableSet.of("true", "on", "yes", "1");
    private static final Set<String> FALSE_VALUES = ImmutableSet.of("false", "off", "no", "0");

    @Override
    public Boolean resolve(
            @Nullable final Annotation annotation,
            @Nonnull final CuracaoContext ctx) throws Exception {
        final HttpRequest request = ctx.getRequest();
        Boolean result = null;
        if (annotation instanceof Query) {
            final Query query = (Query) annotation;
            final String bool = request.getParameter(query.value());
            if (bool == null && query.required()) {
                throw new MissingRequiredParameterException("Request missing required query parameter: "
                        + query.value());
            }
            result = getBooleanFromString(bool);
        } else if (annotation instanceof Path) {
            final String bool =
                    CuracaoContext.Extensions.getPathVariables(ctx).get(((Path) annotation).value());
            result = getBooleanFromString(bool);
        }
        return result;
    }

    @Nullable
    private static Boolean getBooleanFromString(
            @Nullable final String bool) {
        if (bool == null) {
            return null;
        }

        Boolean result = null;
        String value = bool.trim();
        if ("".equals(value)) {
            return null;
        }
        value = value.toLowerCase();
        if (TRUE_VALUES.contains(value)) {
            result = Boolean.TRUE;
        } else if (FALSE_VALUES.contains(value)) {
            result = Boolean.FALSE;
        }
        return result;
    }

}
