package com.kolich.curacao.examples.components;

import com.kolich.curacao.handlers.components.CuracaoComponent;

import javax.servlet.ServletContext;

public abstract class AbstractGsonComponent implements CuracaoComponent {

    @Override
    public void initialize(final ServletContext context) throws Exception {
        // Nothing, intentional.
    }

    @Override
    public void destroy(final ServletContext context) throws Exception {
        // Nothing, intentional.
    }

}
