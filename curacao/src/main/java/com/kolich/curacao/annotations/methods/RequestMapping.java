/**
 * Copyright (c) 2014 Mark S. Kolich
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

package com.kolich.curacao.annotations.methods;

import com.google.common.collect.ImmutableMap;
import com.kolich.curacao.handlers.requests.filters.CuracaoRequestFilter;
import com.kolich.curacao.handlers.requests.filters.DefaultCuracaoRequestFilter;
import com.kolich.curacao.handlers.requests.matchers.CuracaoPathMatcher;
import com.kolich.curacao.handlers.requests.matchers.DefaultCuracaoRegexPathMatcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {

    /**
     * A list of all Curacao supported HTTP methods.  Note that
     * the RFC-2616 technically specifies support for HTTP method
     * "extensions" that basically allow the server to accept
     * request methods of any generic type.  However, lets be real
     * and just acknowledge that the Servlet container may not
     * support request methods of ~any~ arbitrary type.  That said,
     * given Curacao is a toolkit for a Servlet container, we're
     * using an enum here to represent the possible methods that
     * Curacao knows about.  That is, we don't technically support
     * "whatever the consumer" wants, like an arbitrary string, think
     * "FOOBAR /baz HTTP/1.1".  So, if a new method type is to be
     * supported, it needs to be added to this enum at the toolkit
     * level and supported by the Servlet container.
     */
    public static enum RequestMethod {

        TRACE, HEAD, GET, POST, PUT, DELETE;

        // Pre-loaded immutable map, which maps the string equivalent of each
        // HTTP request method to its corresponding enum value.
        private static final ImmutableMap<String, RequestMethod> stringToMethods__ =
            ImmutableMap.<String, RequestMethod>builder()
                .put("TRACE", TRACE)
                .put("HEAD", HEAD)
                .put("GET", GET)
                .put("POST", POST)
                .put("PUT", PUT)
                .put("DELETE", DELETE)
                .build();

        public static final RequestMethod fromString(final String method) {
            return stringToMethods__.get(method); // O(1)
        }

    };

    String value();

    // <https://github.com/markkolich/curacao/issues/2>
    // Default HTTP methods are HEAD and GET, if not specified otherwise.
    RequestMethod[] methods() default {RequestMethod.HEAD, RequestMethod.GET};

    Class<? extends CuracaoPathMatcher> matcher()
        default DefaultCuracaoRegexPathMatcher.class;
	
	Class<? extends CuracaoRequestFilter>[] filters()
		default {DefaultCuracaoRequestFilter.class};

}
