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

package curacao.mcp.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A JSON-RPC 2.0 response: a result, or an error. The {@code id} is always written, as null when
 * the request's id couldn't be read; the result and error are written only when present.
 */
@JsonPropertyOrder({"jsonrpc", "id", "result", "error"})
public interface JsonRpcResponse {

    /**
     * The JSON-RPC version every message carries.
     */
    String VERSION = "2.0";

    @JsonProperty("jsonrpc")
    default String getJsonRpc() {
        return VERSION;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("id")
    JsonNode getId();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("result")
    Object getResult();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("error")
    JsonRpcError getError();

    /**
     * Builds a response, which has either a result or an error.
     */
    final class Builder {

        private JsonNode id_;
        private Object result_;
        private JsonRpcError error_;

        /**
         * The id of the request this answers. An error may leave it unset, when the request's id couldn't be
         * read (a parse error, say), and it's written as null.
         */
        public Builder setId(
                @Nullable final JsonNode id) {
            id_ = id;
            return this;
        }

        public Builder setResult(
                @Nonnull final Object result) {
            result_ = checkNotNull(result, "Result cannot be null.");
            return this;
        }

        public Builder setError(
                @Nonnull final JsonRpcError error) {
            error_ = checkNotNull(error, "Error cannot be null.");
            return this;
        }

        public Builder setError(
                final int code,
                @Nonnull final String message) {
            final JsonRpcError error = new JsonRpcError.Builder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            return setError(error);
        }

        /**
         * Builds the response.
         *
         * @throws IllegalStateException if it has both a result and an error, or neither, or a result
         *     without an id
         */
        public JsonRpcResponse build() {
            checkState((result_ == null) != (error_ == null), "A response has a result or an error, "
                    + "not both or neither.");
            checkState(error_ != null || id_ != null, "A result needs the id of the request it answers.");

            return new JsonRpcResponse() {
                @Override
                public JsonNode getId() {
                    return (id_ == null) ? NullNode.getInstance() : id_;
                }

                @Override
                public Object getResult() {
                    return result_;
                }

                @Override
                public JsonRpcError getError() {
                    return error_;
                }
            };
        }

    }

}
