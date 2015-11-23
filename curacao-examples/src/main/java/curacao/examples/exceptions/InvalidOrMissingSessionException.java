package curacao.examples.exceptions;

import curacao.exceptions.CuracaoException;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

public final class InvalidOrMissingSessionException
    extends CuracaoException.WithStatus {

    private static final long serialVersionUID = -1912974898753262532L;

    public InvalidOrMissingSessionException(final String message) {
        super(SC_UNAUTHORIZED, message, null);
    }

}
