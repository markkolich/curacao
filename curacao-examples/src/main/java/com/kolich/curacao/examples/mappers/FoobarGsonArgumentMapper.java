package com.kolich.curacao.examples.mappers;

import com.google.gson.Gson;
import com.kolich.curacao.annotations.Injectable;
import com.kolich.curacao.annotations.mappers.ControllerArgumentTypeMapper;
import com.kolich.curacao.examples.components.GsonComponent;
import com.kolich.curacao.examples.entities.FoobarGsonEntity;
import com.kolich.curacao.handlers.requests.mappers.types.body.InputStreamReaderRequestMapper;

import java.io.InputStreamReader;

@ControllerArgumentTypeMapper(FoobarGsonEntity.class)
public final class FoobarGsonArgumentMapper
    extends InputStreamReaderRequestMapper<FoobarGsonEntity> {

    private final Gson gson_;

    @Injectable
    public FoobarGsonArgumentMapper(final GsonComponent gson) {
        gson_ = gson.getGsonInstance();
    }

    @Override
    public final FoobarGsonEntity resolveWithReader(
        final InputStreamReader reader) throws Exception {
        return gson_.fromJson(reader, FoobarGsonEntity.class);
    }

}
