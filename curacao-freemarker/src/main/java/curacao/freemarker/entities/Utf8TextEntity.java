/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package curacao.freemarker.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import curacao.entities.AbstractAppendableCuracaoEntity;
import curacao.freemarker.transformers.TextTransformer;
import org.slf4j.Logger;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.Writer;
import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.core.servlet.HttpStatus.SC_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A text response body of a given {@link MediaType}, built with a {@link Builder}. The media type
 * is sent as the {@code Content-Type} exactly as given, and the body is always encoded as UTF-8,
 * so the media type should declare UTF-8 or no charset (e.g. {@link MediaType#HTML_UTF_8}).
 * Pairing a media type with a body (and transformers) that suit it is up to the caller.
 *
 * Before the body is written, it's passed through the entity's {@link TextTransformer}s in the
 * order they were added (e.g. a minifier, then a compressor). With no transformers, it's written
 * as-is. A transformer that throws or returns null is logged at WARN and skipped, so the body it
 * was given is passed on unchanged: transformers are treated as optional polish, never as
 * something the response depends on.
 */
public final class Utf8TextEntity extends AbstractAppendableCuracaoEntity {

    private static final Logger LOG = getLogger(Utf8TextEntity.class);

    private final MediaType mediaType_;
    private final int status_;
    private final String body_;

    private final List<TextTransformer> transformers_;

    private Utf8TextEntity(
            final MediaType mediaType,
            final int status,
            final String body,
            final List<TextTransformer> transformers) {
        super(UTF_8.toString());
        mediaType_ = mediaType;
        status_ = status;
        body_ = body;
        transformers_ = transformers;
    }

    @Override
    public int getStatus() {
        return status_;
    }

    @Override
    public String getContentType() {
        return mediaType_.toString();
    }

    @Override
    public void toWriter(
            final Writer writer) throws Exception {
        final String body = transformers_.stream()
                .map(Utf8TextEntity::skipOnFailure)
                .reduce(Function.identity(), Function::andThen)
                .apply(body_);
        writer.write(body);
    }

    /**
     * Wraps a transformer so that a failure doesn't lose the response: if it throws or returns null,
     * the failure is logged and the body it was given is passed on unchanged.
     */
    private static Function<String, String> skipOnFailure(
            final TextTransformer transformer) {
        return body -> {
            try {
                final String transformed = transformer.apply(body);
                if (transformed != null) {
                    return transformed;
                }
                LOG.warn("Transformer returned a null body; skipping it: {}", transformer);
            } catch (final Exception e) {
                LOG.warn("Transformer failed; skipping it: {}", transformer, e);
            }
            return body;
        };
    }

    public static final class Builder {

        private final MediaType mediaType_;
        private int status_ = SC_OK;
        private String body_;

        private final ImmutableList.Builder<TextTransformer> transformers_ = ImmutableList.builder();

        /**
         * Starts an entity of the given media type.
         *
         * @param mediaType the media type, e.g. {@link MediaType#HTML_UTF_8}
         */
        public Builder(
                @Nonnull final MediaType mediaType) {
            mediaType_ = checkNotNull(mediaType, "Media type cannot be null.");
        }

        public Builder setStatus(
                @Nonnegative final int status) {
            checkArgument(status >= SC_OK, "HTTP response status cannot be < " + SC_OK);
            status_ = status;
            return this;
        }

        public Builder setBody(
                @Nonnull final String body) {
            body_ = checkNotNull(body, "Body cannot be null.");
            return this;
        }

        public Builder addTransformer(
                @Nonnull final TextTransformer transformer) {
            transformers_.add(checkNotNull(transformer, "Transformer cannot be null."));
            return this;
        }

        public Builder addTransformers(
                @Nonnull final Iterable<? extends TextTransformer> transformers) {
            transformers_.addAll(checkNotNull(transformers, "Transformers cannot be null."));
            return this;
        }

        public Utf8TextEntity build() {
            checkNotNull(body_, "Body cannot be null.");

            return new Utf8TextEntity(mediaType_, status_, body_, transformers_.build());
        }

    }

}
