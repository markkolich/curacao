package com.kolich.curacao.examples.components;

import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.annotations.Injectable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class exists to make sure that Curacao can tell the difference
 * between "com.kolich.curacao.examples.components.UserAuthenticator" and
 * this class "com.kolich.curacao.examples.components.UserAuthenticatorConfig".
 * See https://github.com/markkolich/curacao/issues/8
 */
@Component
public final class UserAuthenticatorConfig {

    private final GsonComponent gson_;
    private final SessionCache sessionCache_;

    @Injectable
    public UserAuthenticatorConfig(final GsonComponent gson,
                                   final SessionCache sessionCache) {
        gson_ = checkNotNull(gson, "GSON instance cannot be null.");
        sessionCache_ = checkNotNull(sessionCache, "Session cache cannot " +
            "be null.");
    }

    public GsonComponent getGson() {
        return gson_;
    }

    public SessionCache getSesssionCache() {
        return sessionCache_;
    }

}
