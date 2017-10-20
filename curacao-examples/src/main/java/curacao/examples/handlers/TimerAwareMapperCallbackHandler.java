package curacao.examples.handlers;

import curacao.CuracaoContext;
import curacao.handlers.AbstractContextCompletingCallbackHandler;
import curacao.handlers.ReturnTypeMapperCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public final class TimerAwareMapperCallbackHandler extends AbstractContextCompletingCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(TimerAwareMapperCallbackHandler.class);

    private final ReturnTypeMapperCallbackHandler delegate_;

    public TimerAwareMapperCallbackHandler(@Nonnull final CuracaoContext ctx) {
        super(ctx);
        delegate_ = new ReturnTypeMapperCallbackHandler(ctx_);
    }

    @Override
    public void renderSuccess(@Nonnull final Object result) throws Exception {
        try {
            delegate_.renderSuccess(result);
        } finally {
            log.info("Request took {}-ms.", (System.currentTimeMillis()-ctx_.creationTime_));
        }
    }

    @Override
    public void renderFailure(@Nonnull final Throwable t) throws Exception {
        try {
            delegate_.renderFailure(t);
        } finally {
            log.info("Request took {}-ms.", (System.currentTimeMillis()-ctx_.creationTime_));
        }
    }

}
