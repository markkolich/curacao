package curacao.examples.components.suppress;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.exceptions.CuracaoException;

@Component
public final class CountryConsumer {

    @Injectable
    public CountryConsumer(final Country suppressed) {
        if (!(suppressed instanceof UnitedStates)) {
            throw new CuracaoException("Should never get here; expecting: "
                    + UnitedStates.class.getCanonicalName());
        }
    }

}
