/**
 * Copyright (c) 2013 Mark S. Kolich
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

package com.kolich.curacao.handlers.requests.mappers.types.body;

import com.google.common.collect.Multimap;
import com.kolich.curacao.annotations.parameters.RequestBody;

import static com.google.common.collect.Iterables.getFirst;

public final class RequestBodyParameterMapper
    extends EncodedRequestBodyMapper<String> {

    @Override
    public final String resolveWithMultimap(final RequestBody annotation,
        final Multimap<String,String> map) throws Exception {
        final String value = annotation.value();
        // If the "value" attached to the request body annotation is something
        // other than "" (empty string) then we should attempt to extract
        // the value that corresponds to the key.  It should be noted that
        // map.get() is a "get" on a Multimap, which is guaranteed to return
        // an empty collection (not null) if the map doesn't contain any values
        // for the given key.
        return ("".equals(value)) ? null : getFirst(map.get(value), null);
    }

}
