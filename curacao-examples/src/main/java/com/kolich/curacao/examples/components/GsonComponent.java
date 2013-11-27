package com.kolich.curacao.examples.components;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.handlers.components.CuracaoComponent;

@Component
public final class GsonComponent implements CuracaoComponent {

    public final Gson getGsonInstance() {
        return new GsonBuilder().serializeNulls().create();
    }

    @Override
    public void initialize() throws Exception {
        // Nothing, intentional.
    }

    @Override
    public void destroy() throws Exception {
        // Nothing, intentional.
    }

}
