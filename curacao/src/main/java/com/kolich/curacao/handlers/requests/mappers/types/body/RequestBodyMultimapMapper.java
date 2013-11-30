package com.kolich.curacao.handlers.requests.mappers.types.body;

import com.google.common.collect.Multimap;
import com.kolich.curacao.annotations.parameters.RequestBody;

public final class RequestBodyMultimapMapper
    extends EncodedRequestBodyMapper<Multimap<String,String>> {

    @Override
    public Multimap<String,String> resolveWithMultimap(
        final RequestBody annotation, final Multimap<String,String> map)
        throws Exception {
        return map;
    }

}
