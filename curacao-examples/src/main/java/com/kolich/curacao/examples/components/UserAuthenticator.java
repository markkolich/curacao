package com.kolich.curacao.examples.components;

import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.handlers.components.CuracaoComponent;

@Component
public final class UserAuthenticator implements CuracaoComponent {

    public final boolean isValidLogin(final String username, final String password) {
        return "foo".equals(username) && "bar".equals(password);
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
