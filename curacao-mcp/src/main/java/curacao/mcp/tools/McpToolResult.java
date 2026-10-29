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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * The result of a tool call: its text content, its structured content if the tool has an output
 * schema, and whether the call failed. A failed call is still a result, not a JSON-RPC error, so
 * the model can read what went wrong and adjust.
 */
@JsonPropertyOrder({"content", "structuredContent", "isError"})
public interface McpToolResult {

    @JsonProperty("content")
    List<Content> getContent();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("structuredContent")
    Object getStructuredContent();

    @JsonProperty("isError")
    boolean isError();

    /**
     * A text content block.
     */
    final class Content {

        private static final String TEXT = "text";

        private final String text_;

        private Content(
                final String text) {
            text_ = text;
        }

        @JsonProperty("type")
        public String getType() {
            return TEXT;
        }

        @JsonProperty("text")
        public String getText() {
            return text_;
        }

    }

    /**
     * Builds a result. It needs text; a tool with an output schema also sets structured content, and a failed
     * call is marked with {@link #setError(boolean)}.
     */
    final class Builder {

        private String text_;
        private Object structuredContent_;
        private boolean error_;

        /**
         * The result as text for the model, often JSON; for a failed call, a message the model can act on.
         */
        public Builder setText(
                @Nonnull final String text) {
            text_ = checkNotNull(text, "Tool result text cannot be null.");
            return this;
        }

        /**
         * The structured result, for a tool with an output schema: anything Jackson writes as a JSON object
         * matching the schema, such as a map, a bean or an {@code ObjectNode}. The specification says the text
         * should be the same JSON, serialized, which {@link #setStructuredContent(Object, ObjectMapper)} does
         * for you.
         */
        public Builder setStructuredContent(
                @Nonnull final Object structuredContent) {
            structuredContent_ = checkNotNull(structuredContent, "Structured content cannot be null.");
            return this;
        }

        /**
         * The structured result, and the same thing written by the given mapper as the text, for clients that
         * don't read structured content.
         */
        public Builder setStructuredContent(
                @Nonnull final Object structuredContent,
                @Nonnull final ObjectMapper objectMapper) throws JsonProcessingException {
            checkNotNull(structuredContent, "Structured content cannot be null.");
            checkNotNull(objectMapper, "Object mapper cannot be null.");
            structuredContent_ = structuredContent;
            text_ = objectMapper.writeValueAsString(structuredContent);
            return this;
        }

        /**
         * Whether the call failed. A failed call is still a result, with {@code isError} set, so the model can
         * read the text and adjust. Defaults to false.
         */
        public Builder setError(
                final boolean error) {
            error_ = error;
            return this;
        }

        /**
         * Builds the result.
         *
         * @throws IllegalStateException if no text was set
         */
        public McpToolResult build() {
            checkState(text_ != null, "A tool result needs text.");

            return new McpToolResult() {
                @Override
                public List<Content> getContent() {
                    return ImmutableList.of(new Content(text_));
                }

                @Override
                public Object getStructuredContent() {
                    return structuredContent_;
                }

                @Override
                public boolean isError() {
                    return error_;
                }
            };
        }

    }

}
