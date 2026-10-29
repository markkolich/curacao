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

package curacao.mcp.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Nullable;

/**
 * An MCP tool's behavior hints. Any hint left unset is omitted, and clients assume the
 * specification's default for it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"readOnlyHint", "destructiveHint", "idempotentHint", "openWorldHint"})
public interface McpToolAnnotations {

    /**
     * The tool doesn't change its environment.
     */
    @Nullable
    @JsonProperty("readOnlyHint")
    Boolean getReadOnlyHint();

    /**
     * The tool may delete or overwrite (meaningful only when it isn't read-only).
     */
    @Nullable
    @JsonProperty("destructiveHint")
    Boolean getDestructiveHint();

    /**
     * Calling the tool again with the same arguments has no further effect.
     */
    @Nullable
    @JsonProperty("idempotentHint")
    Boolean getIdempotentHint();

    /**
     * The tool reaches outside the server's own data (the web, say).
     */
    @Nullable
    @JsonProperty("openWorldHint")
    Boolean getOpenWorldHint();

    final class Builder {

        private Boolean readOnlyHint_;
        private Boolean destructiveHint_;
        private Boolean idempotentHint_;
        private Boolean openWorldHint_;

        public Builder setReadOnlyHint(
                final boolean readOnlyHint) {
            readOnlyHint_ = readOnlyHint;
            return this;
        }

        public Builder setDestructiveHint(
                final boolean destructiveHint) {
            destructiveHint_ = destructiveHint;
            return this;
        }

        public Builder setIdempotentHint(
                final boolean idempotentHint) {
            idempotentHint_ = idempotentHint;
            return this;
        }

        public Builder setOpenWorldHint(
                final boolean openWorldHint) {
            openWorldHint_ = openWorldHint;
            return this;
        }

        public McpToolAnnotations build() {
            return new McpToolAnnotations() {
                @Override
                public Boolean getReadOnlyHint() {
                    return readOnlyHint_;
                }

                @Override
                public Boolean getDestructiveHint() {
                    return destructiveHint_;
                }

                @Override
                public Boolean getIdempotentHint() {
                    return idempotentHint_;
                }

                @Override
                public Boolean getOpenWorldHint() {
                    return openWorldHint_;
                }
            };
        }

    }

}
