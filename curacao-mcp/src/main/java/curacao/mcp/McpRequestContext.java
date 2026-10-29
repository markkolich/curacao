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

package curacao.mcp;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * What a tool or prompt knows about the request that called it: the HTTP headers, the protocol
 * version the client sent, and any attributes the application attached with a
 * {@link curacao.mcp.http.McpRequestContextExtractor}. Empty when the server is called
 * directly, as in tests.
 */
public final class McpRequestContext {

    /**
     * No headers, no protocol version and no attributes.
     */
    public static final McpRequestContext EMPTY = new Builder().build();

    private final ImmutableListMultimap<String, String> headers_;
    private final String protocolVersion_;
    private final Map<String, Object> attributes_;

    private McpRequestContext(
            final Builder builder) {
        headers_ = builder.headers_.build();
        protocolVersion_ = builder.protocolVersion_;
        attributes_ = builder.attributes_.build();
    }

    /**
     * A header's first value, or null; names are case-insensitive.
     */
    @Nullable
    public String getHeader(
            @Nonnull final String name) {
        final List<String> values = getHeaders(name);
        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * Every value of a header; names are case-insensitive.
     */
    public List<String> getHeaders(
            @Nonnull final String name) {
        checkNotNull(name, "Header name cannot be null.");
        return headers_.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * The client's {@code MCP-Protocol-Version} header, or null when it sent none.
     */
    @Nullable
    public String getProtocolVersion() {
        return protocolVersion_;
    }

    public Map<String, Object> getAttributes() {
        return attributes_;
    }

    /**
     * An attribute, or null when it's absent or isn't of the given type.
     */
    @Nullable
    public <T> T getAttribute(
            @Nonnull final String name,
            @Nonnull final Class<T> type) {
        checkNotNull(name, "Attribute name cannot be null.");
        checkNotNull(type, "Attribute type cannot be null.");
        final Object value = attributes_.get(name);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public static final class Builder {

        private final ImmutableListMultimap.Builder<String, String> headers_ = ImmutableListMultimap.builder();
        private String protocolVersion_;
        private final ImmutableMap.Builder<String, Object> attributes_ = ImmutableMap.builder();

        public Builder addHeader(
                @Nonnull final String name,
                @Nonnull final String value) {
            checkNotNull(name, "Header name cannot be null.");
            checkNotNull(value, "Header value cannot be null.");
            headers_.put(name.toLowerCase(Locale.ROOT), value);
            return this;
        }

        public Builder setProtocolVersion(
                @Nullable final String protocolVersion) {
            protocolVersion_ = protocolVersion;
            return this;
        }

        public Builder putAttribute(
                @Nonnull final String name,
                @Nonnull final Object value) {
            checkNotNull(name, "Attribute name cannot be null.");
            checkNotNull(value, "Attribute value cannot be null.");
            attributes_.put(name, value);
            return this;
        }

        public Builder putAttributes(
                @Nonnull final Map<String, ?> attributes) {
            checkNotNull(attributes, "Attributes cannot be null.");
            attributes_.putAll(attributes);
            return this;
        }

        public McpRequestContext build() {
            return new McpRequestContext(this);
        }

    }

}
