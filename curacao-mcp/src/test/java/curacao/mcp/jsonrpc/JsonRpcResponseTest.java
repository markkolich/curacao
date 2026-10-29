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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonRpcResponseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void writesAResultTest() {
        final JsonNode json = MAPPER.valueToTree(new JsonRpcResponse.Builder()
                .setId(MAPPER.getNodeFactory().numberNode(1))
                .setResult(MAPPER.createObjectNode().put("ok", true))
                .build());

        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"ok\":true}}", json.toString());
    }

    @Test
    public void writesAnErrorWithANullIdTest() {
        final JsonNode json = MAPPER.valueToTree(new JsonRpcResponse.Builder()
                .setError(JsonRpcError.PARSE_ERROR, "Parse error.")
                .build());

        assertTrue(json.has("id"));
        assertTrue(json.path("id").isNull());
        assertEquals(JsonRpcError.PARSE_ERROR, json.at("/error/code").asInt());
        assertFalse(json.has("result"));
    }

    @Test
    public void rejectsInvalidResponsesTest() {
        // Neither a result nor an error.
        assertThrows(IllegalStateException.class, () -> new JsonRpcResponse.Builder()
                .setId(MAPPER.getNodeFactory().numberNode(1))
                .build());
        // Both.
        assertThrows(IllegalStateException.class, () -> new JsonRpcResponse.Builder()
                .setId(MAPPER.getNodeFactory().numberNode(1))
                .setResult(MAPPER.createObjectNode())
                .setError(JsonRpcError.INTERNAL_ERROR, "Boom.")
                .build());
        // A result without an id.
        assertThrows(IllegalStateException.class, () -> new JsonRpcResponse.Builder()
                .setResult(MAPPER.createObjectNode())
                .build());
    }

}
