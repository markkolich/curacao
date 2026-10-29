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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Nullable;

/**
 * One JSON-RPC 2.0 message from an MCP client: a request (with an {@code id}), a notification
 * (without one), or a client's response to a server request (with an {@code id} but no
 * {@code method}). Jackson reads one from JSON through its {@link Builder}.
 */
@JsonDeserialize(builder = JsonRpcRequest.Builder.class)
public interface JsonRpcRequest {

    @Nullable
    @JsonProperty("jsonrpc")
    String getJsonRpc();

    @Nullable
    @JsonProperty("id")
    JsonNode getId();

    @Nullable
    @JsonProperty("method")
    String getMethod();

    @Nullable
    @JsonProperty("params")
    JsonNode getParams();

    /**
     * True for a message that gets no response: a notification, or a client's response to a
     * server request.
     */
    default boolean expectsNoResponse() {
        final JsonNode id = getId();
        return id == null || id.isNull() || getMethod() == null;
    }

    /**
     * Builds a message. Every field is optional, since the server answers a malformed message with an error
     * rather than failing to read it; unknown JSON properties are ignored.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    final class Builder {

        private String jsonrpc_;
        private JsonNode id_;
        private String method_;
        private JsonNode params_;

        /**
         * The protocol version, which must be {@code "2.0"}.
         */
        @JsonProperty("jsonrpc")
        public Builder setJsonRpc(
                @Nullable final String jsonrpc) {
            jsonrpc_ = jsonrpc;
            return this;
        }

        /**
         * The request's id, a string or a number; none for a notification.
         */
        @JsonProperty("id")
        public Builder setId(
                @Nullable final JsonNode id) {
            id_ = id;
            return this;
        }

        @JsonProperty("method")
        public Builder setMethod(
                @Nullable final String method) {
            method_ = method;
            return this;
        }

        /**
         * The method's parameters, if any.
         */
        @JsonProperty("params")
        public Builder setParams(
                @Nullable final JsonNode params) {
            params_ = params;
            return this;
        }

        public JsonRpcRequest build() {
            return new JsonRpcRequest() {
                @Override
                public String getJsonRpc() {
                    return jsonrpc_;
                }

                @Override
                public JsonNode getId() {
                    return id_;
                }

                @Override
                public String getMethod() {
                    return method_;
                }

                @Override
                public JsonNode getParams() {
                    return params_;
                }
            };
        }

    }

}
