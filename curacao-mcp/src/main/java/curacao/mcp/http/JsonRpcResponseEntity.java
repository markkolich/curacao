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

package curacao.mcp.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.entities.AbstractAppendableCuracaoEntity;
import curacao.mcp.jsonrpc.JsonRpcResponse;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static curacao.core.servlet.HttpStatus.SC_OK;

/**
 * A JSON-RPC response, sent as JSON with an HTTP status.
 */
public final class JsonRpcResponseEntity extends AbstractAppendableCuracaoEntity {

    private static final String JSON_UTF_8_TYPE = JSON_UTF_8.toString();

    private final transient ObjectMapper objectMapper_;
    private final int status_;
    private final JsonRpcResponse response_;

    private JsonRpcResponseEntity(
            final ObjectMapper objectMapper,
            final int status,
            final JsonRpcResponse response) {
        objectMapper_ = objectMapper;
        status_ = status;
        response_ = response;
    }

    public JsonRpcResponse getResponse() {
        return response_;
    }

    @Override
    public int getStatus() {
        return status_;
    }

    @Override
    public String getContentType() {
        return JSON_UTF_8_TYPE;
    }

    @Override
    public void toWriter(
            final Writer writer) throws Exception {
        objectMapper_.writeValue(writer, response_);
    }

    public static final class Builder {

        private final ObjectMapper objectMapper_;
        private int status_ = SC_OK;
        private JsonRpcResponse response_;

        /**
         * Starts an entity written with the given mapper.
         *
         * @param objectMapper the mapper that writes the response
         */
        public Builder(
                @Nonnull final ObjectMapper objectMapper) {
            objectMapper_ = checkNotNull(objectMapper, "Object mapper cannot be null.");
        }

        /**
         * The HTTP status. Defaults to 200.
         */
        public Builder setStatus(
                @Nonnegative final int status) {
            checkArgument(status >= SC_OK, "HTTP response status cannot be < " + SC_OK);
            status_ = status;
            return this;
        }

        public Builder setResponse(
                @Nonnull final JsonRpcResponse response) {
            response_ = checkNotNull(response, "Response cannot be null.");
            return this;
        }

        public JsonRpcResponseEntity build() {
            checkNotNull(response_, "Response cannot be null.");
            return new JsonRpcResponseEntity(objectMapper_, status_, response_);
        }

    }

}
