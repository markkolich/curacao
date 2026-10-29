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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class McpToolResultTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void textResultTest() {
        final McpToolResult result = new McpToolResult.Builder()
                .setText("hello")
                .build();

        assertEquals("hello", result.getContent().get(0).getText());
        assertNull(result.getStructuredContent());
        assertFalse(result.isError());
    }

    @Test
    public void structuredResultWritesItsTextTest() throws Exception {
        final Map<String, Object> sum = ImmutableMap.of("sum", 3);
        final McpToolResult result = new McpToolResult.Builder()
                .setStructuredContent(sum, MAPPER)
                .build();

        assertSame(sum, result.getStructuredContent());
        assertEquals("{\"sum\":3}", result.getContent().get(0).getText());
    }

    @Test
    public void errorResultTest() {
        final McpToolResult result = new McpToolResult.Builder()
                .setText("Unknown time zone: Mars/Olympus")
                .setError(true)
                .build();

        assertTrue(result.isError());
        final String expected = "{\"content\":[{\"type\":\"text\",\"text\":\"Unknown time zone: Mars/Olympus\"}],"
                + "\"isError\":true}";
        assertEquals(expected, MAPPER.valueToTree(result).toString());
    }

    @Test
    public void needsTextTest() {
        assertThrows(IllegalStateException.class, () -> new McpToolResult.Builder()
                .setStructuredContent(ImmutableMap.of("sum", 3))
                .build());
    }

}
