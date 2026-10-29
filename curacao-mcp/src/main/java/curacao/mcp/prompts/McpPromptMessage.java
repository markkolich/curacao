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

package curacao.mcp.prompts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * One text message of a prompt, from the user or the assistant.
 */
@JsonPropertyOrder({"role", "content"})
public interface McpPromptMessage {

    /**
     * Who a message is from, written as {@code "user"} or {@code "assistant"}.
     */
    enum Role {

        USER, ASSISTANT;

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }

    }

    @JsonProperty("role")
    Role getRole();

    @JsonProperty("content")
    Map<String, String> getContent();

    final class Builder {

        private static final String TYPE = "type";
        private static final String TEXT = "text";

        private Role role_ = Role.USER;
        private String text_;

        /**
         * Who the message is from; defaults to {@link Role#USER}.
         */
        public Builder setRole(
                @Nonnull final Role role) {
            role_ = checkNotNull(role, "Prompt message role cannot be null.");
            return this;
        }

        public Builder setText(
                @Nonnull final String text) {
            text_ = checkNotNull(text, "Prompt message text cannot be null.");
            return this;
        }

        /**
         * Builds the message.
         *
         * @throws IllegalStateException if no text was set
         */
        public McpPromptMessage build() {
            checkState(text_ != null, "A prompt message needs text.");

            return new McpPromptMessage() {
                @Override
                public Role getRole() {
                    return role_;
                }

                @Override
                public Map<String, String> getContent() {
                    return ImmutableMap.of(TYPE, TEXT, TEXT, text_);
                }
            };
        }

    }

}
