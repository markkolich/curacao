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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class McpPromptMessageTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void writesAUserMessageByDefaultTest() {
        final McpPromptMessage message = new McpPromptMessage.Builder()
                .setText("Hello!")
                .build();

        assertEquals("{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"Hello!\"}}",
                MAPPER.valueToTree(message).toString());
    }

    @Test
    public void writesAnAssistantMessageTest() {
        final McpPromptMessage message = new McpPromptMessage.Builder()
                .setRole(McpPromptMessage.Role.ASSISTANT)
                .setText("Hi there.")
                .build();

        assertEquals("assistant", MAPPER.valueToTree(message).path("role").asText());
    }

    @Test
    public void needsTextTest() {
        assertThrows(IllegalStateException.class, () -> new McpPromptMessage.Builder().build());
    }

}
