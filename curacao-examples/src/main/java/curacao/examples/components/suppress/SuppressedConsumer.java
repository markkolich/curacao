package curacao.examples.components.suppress;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.exceptions.CuracaoException;

import javax.annotation.Nonnull;

@Component
public final class SuppressedConsumer {

    @Injectable
    public SuppressedConsumer(
            @Nonnull final Suppressed suppressed) {
        if (!(suppressed instanceof MockSuppressedComponent)) {
            throw new CuracaoException("Should never get here; expecting: "
                    + MockSuppressedComponent.class.getCanonicalName());
        }
    }

}
