package curacao.examples.components;

import curacao.annotations.Component;
import curacao.annotations.Injectable;

import javax.annotation.Nonnull;

/**
 * This class exists to make sure that Curacao can tell the difference
 * between "curacao.examples.components.UserAuthenticator" and
 * this class "curacao.examples.components.UserAuthenticatorConfig".
 * See https://github.com/markkolich/curacao/issues/8
 */
@Component
public final class UserAuthenticatorConfig {

    private final GsonComponent gson_;
    private final SessionCache sessionCache_;

    @Injectable
    public UserAuthenticatorConfig(@Nonnull final GsonComponent gson,
                                   @Nonnull final SessionCache sessionCache) {
        gson_ = gson;
        sessionCache_ = sessionCache;
    }

    public GsonComponent getGson() {
        return gson_;
    }

    public SessionCache getSesssionCache() {
        return sessionCache_;
    }

}
