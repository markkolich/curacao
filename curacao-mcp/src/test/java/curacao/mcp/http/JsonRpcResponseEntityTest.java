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
import curacao.mcp.jsonrpc.JsonRpcError;
import curacao.mcp.jsonrpc.JsonRpcResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class JsonRpcResponseEntityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void defaultsToOkTest() throws Exception {
        final JsonRpcResponse response = new JsonRpcResponse.Builder()
                .setId(MAPPER.getNodeFactory().numberNode(1))
                .setResult(MAPPER.createObjectNode())
                .build();
        final JsonRpcResponseEntity entity = new JsonRpcResponseEntity.Builder(MAPPER)
                .setResponse(response)
                .build();

        assertEquals(200, entity.getStatus());
        assertEquals("application/json; charset=utf-8", entity.getContentType());
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}", render(entity));
    }

    @Test
    public void errorWithStatusTest() throws Exception {
        final JsonRpcResponseEntity entity = new JsonRpcResponseEntity.Builder(MAPPER)
                .setStatus(403)
                .setResponse(new JsonRpcResponse.Builder().setError(JsonRpcError.INVALID_REQUEST, "Nope.").build())
                .build();

        assertEquals(403, entity.getStatus());
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Nope.\"}}",
                render(entity));
    }

    @Test
    public void invalidEntityTest() {
        assertThrows(IllegalArgumentException.class,
                () -> new JsonRpcResponseEntity.Builder(MAPPER).setStatus(199));
        assertThrows(NullPointerException.class,
                () -> new JsonRpcResponseEntity.Builder(MAPPER).build());
    }

    private static String render(
            final JsonRpcResponseEntity entity) throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        entity.write(os);

        return os.toString(StandardCharsets.UTF_8);
    }

}
