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
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import curacao.freemarker.transformers.TextTransformer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static curacao.core.servlet.HttpStatus.SC_OK;

/**
 * A FreeMarker template to render, along with the data handed to it. Controllers return these,
 * and a {@link curacao.freemarker.mappers.AbstractFreeMarkerContentResponseMapper} renders them.
 */
public interface FreeMarkerContent {

    String getTemplateName();

    MediaType getMediaType();

    int getStatus();

    @Nonnull
    default Map<String, Object> getDataMap() {
        return ImmutableMap.of();
    }

    @Nonnull
    default List<TextTransformer> getTransformers() {
        return ImmutableList.of();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    default <T> T getAttribute(
            final String name) {
        checkNotNull(name, "Attribute name cannot be null.");

        return (T) getDataMap().get(name);
    }

    final class Builder {

        private final String templateName_;
        private final MediaType mediaType_;
        private final int status_;

        /**
         * Note, {@link ImmutableMap.Builder}'s do not support multiple calls to
         * {@link ImmutableMap.Builder#put(Object, Object)} with the same key. If the consumer/caller
         * of this builder invokes <code>put(x, y);</code> followed by a <code>put(x, z);</code> the
         * internal call to {@link ImmutableMap.Builder#build()} will fail hard at builder-build time
         * because of the duplicate keys. This is expected and avoids bugs/mistakes in creating the
         * internal data map.
         */
        private final ImmutableMap.Builder<String, Object> dataMap_ = ImmutableMap.builder();

        private final ImmutableList.Builder<TextTransformer> transformers_ = ImmutableList.builder();

        public Builder(
                final String templateName,
                final MediaType mediaType,
                final int status) {
            templateName_ = checkNotNull(templateName, "Template name cannot be null.");
            mediaType_ = checkNotNull(mediaType, "Media type cannot be null.");
            checkState(status >= SC_OK, "HTTP response status attached to template cannot be < " + SC_OK);
            status_ = status;
        }

        public Builder(
                final String templateName,
                final int status) {
            this(templateName, MediaType.HTML_UTF_8, status);
        }

        public Builder(
                final String templateName,
                final MediaType mediaType) {
            this(templateName, mediaType, SC_OK);
        }

        public Builder(
                final String templateName) {
            this(templateName, SC_OK);
        }

        public Builder withAttr(
                @Nullable final String name,
                @Nullable final Object value) {
            if (name != null && value != null) {
                dataMap_.put(name, value);
            }
            return this;
        }

        /**
         * Adds transformers to run, in order, on the rendered body. Calling this more than once
         * appends to the transformers already added.
         *
         * @param transformers the transformers to run, in order
         * @return this builder
         */
        public Builder withTransformers(
                @Nonnull final TextTransformer... transformers) {
            transformers_.add(checkNotNull(transformers, "Transformers cannot be null."));
            return this;
        }

        public FreeMarkerContent build() {
            final Map<String, Object> dataMap = dataMap_.build();
            final List<TextTransformer> transformers = transformers_.build();
            return new FreeMarkerContent() {
                @Override
                public String getTemplateName() {
                    return templateName_;
                }

                @Override
                public MediaType getMediaType() {
                    return mediaType_;
                }

                @Override
                public int getStatus() {
                    return status_;
                }

                @Nonnull
                @Override
                public Map<String, Object> getDataMap() {
                    return dataMap;
                }

                @Nonnull
                @Override
                public List<TextTransformer> getTransformers() {
                    return transformers;
                }
            };
        }

    }

}
