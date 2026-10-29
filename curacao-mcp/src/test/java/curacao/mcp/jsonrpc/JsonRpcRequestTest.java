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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonRpcRequestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void readsARequestThroughTheBuilderTest() throws Exception {
        final JsonRpcRequest request = MAPPER.readValue("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"echo\"},\"unknown\":true}", JsonRpcRequest.class);

        assertEquals("2.0", request.getJsonRpc());
        assertEquals(7, request.getId().asInt());
        assertEquals("tools/call", request.getMethod());
        assertEquals("echo", request.getParams().path("name").asText());
        assertFalse(request.expectsNoResponse());
    }

    @Test
    public void readsANotificationTest() throws Exception {
        final JsonRpcRequest request = MAPPER.readValue(
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", JsonRpcRequest.class);

        assertNull(request.getId());
        assertNull(request.getParams());
        assertTrue(request.expectsNoResponse());
    }

    @Test
    public void nullIdGetsNoResponseTest() throws Exception {
        final JsonRpcRequest request = MAPPER.readValue("{\"jsonrpc\":\"2.0\",\"id\":null,\"method\":\"ping\"}",
                JsonRpcRequest.class);

        assertTrue(request.expectsNoResponse());
    }

}
