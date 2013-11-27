package com.kolich.curacao.examples.mappers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kolich.curacao.annotations.mappers.ControllerArgumentTypeMapper;
import com.kolich.curacao.examples.entities.FoobarGsonEntity;
import com.kolich.curacao.handlers.requests.mappers.types.body.InputStreamReaderRequestMapper;

import java.io.InputStreamReader;

@ControllerArgumentTypeMapper(FoobarGsonEntity.class)
public final class FoobarGsonArgumentMapper
    extends InputStreamReaderRequestMapper<FoobarGsonEntity> {

    private final Gson gson_;

    public FoobarGsonArgumentMapper() {
        gson_ = new GsonBuilder().serializeNulls().create();
    }

    @Override
    public final FoobarGsonEntity resolveWithReader(
        final InputStreamReader reader) throws Exception {
        return gson_.fromJson(reader, FoobarGsonEntity.class);
    }

}
