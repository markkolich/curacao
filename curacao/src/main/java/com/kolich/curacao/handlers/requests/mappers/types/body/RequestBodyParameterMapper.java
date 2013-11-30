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
