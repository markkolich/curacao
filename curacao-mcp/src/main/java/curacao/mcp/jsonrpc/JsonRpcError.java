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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * The error member of a JSON-RPC 2.0 response, and the error codes the specification reserves.
 */
@JsonPropertyOrder({"code", "message"})
public interface JsonRpcError {

    /**
     * The message isn't valid JSON.
     */
    int PARSE_ERROR = -32700;
    /**
     * The JSON isn't a valid JSON-RPC request.
     */
    int INVALID_REQUEST = -32600;
    int METHOD_NOT_FOUND = -32601;
    int INVALID_PARAMS = -32602;
    int INTERNAL_ERROR = -32603;
    /**
     * The start of the range (-32000 to -32099) reserved for implementation-defined server errors.
     */
    int SERVER_ERROR = -32000;

    @JsonProperty("code")
    int getCode();

    @JsonProperty("message")
    String getMessage();

    final class Builder {

        private Integer code_;
        private String message_;

        /**
         * The error code: one of the codes above, or one of the application's own.
         */
        public Builder setCode(
                final int code) {
            code_ = code;
            return this;
        }

        public Builder setMessage(
                @Nonnull final String message) {
            message_ = checkNotNull(message, "Error message cannot be null.");
            return this;
        }

        /**
         * Builds the error.
         *
         * @throws IllegalStateException if no code or no message was set
         */
        public JsonRpcError build() {
            checkState(code_ != null, "A JSON-RPC error needs a code.");
            checkState(message_ != null, "A JSON-RPC error needs a message.");

            return new JsonRpcError() {
                @Override
                public int getCode() {
                    return code_;
                }

                @Override
                public String getMessage() {
                    return message_;
                }
            };
        }

    }

}
