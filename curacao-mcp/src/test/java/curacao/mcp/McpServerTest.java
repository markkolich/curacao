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

package curacao.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import curacao.mcp.exceptions.CuracaoMcpException;
import curacao.mcp.jsonrpc.JsonRpcError;
import curacao.mcp.jsonrpc.JsonRpcRequest;
import curacao.mcp.jsonrpc.JsonRpcResponse;
import curacao.mcp.prompts.McpPrompt;
import curacao.mcp.prompts.McpPromptArgument;
import curacao.mcp.prompts.McpPromptMessage;
import curacao.mcp.tools.McpTool;
import curacao.mcp.tools.McpToolAnnotations;
import curacao.mcp.tools.McpToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class McpServerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final McpTool ECHO = new McpTool() {
        @Override
        public String getName() {
            return "echo_text";
        }

        @Override
        public String getDescription() {
            return "Echoes its text.";
        }

        @Override
        public Map<String, Object> getInputProperties() {
            return ImmutableMap.of("text", ImmutableMap.of("type", "string"));
        }

        @Override
        public List<String> getRequiredProperties() {
            return ImmutableList.of("text");
        }

        @Override
        public McpToolAnnotations getAnnotations() {
            return new McpToolAnnotations.Builder().setReadOnlyHint(true).setOpenWorldHint(false).build();
        }

        @Override
        public McpToolResult call(
                final Map<String, Object> arguments,
                final McpRequestContext context) {
            final Object text = arguments.get("text");
            if ("boom".equals(text)) {
                throw new IllegalStateException("exploded");
            } else if (!(text instanceof String)) {
                throw new CuracaoMcpException(JsonRpcError.INVALID_PARAMS, "text is required.");
            }
            return new McpToolResult.Builder()
                    .setText((String) text)
                    .build();
        }
    };

    private static final McpPrompt HELLO = new McpPrompt() {
        @Override
        public String getName() {
            return "hello";
        }

        @Override
        public String getDescription() {
            return "Says hello.";
        }

        @Override
        public List<McpPromptArgument> getArguments() {
            final McpPromptArgument name = new McpPromptArgument.Builder()
                    .setName("name")
                    .setDescription("Who to greet.")
                    .setRequired(true)
                    .build();
            final McpPromptArgument mood = new McpPromptArgument.Builder()
                    .setName("mood")
                    .build();
            return ImmutableList.of(name, mood);
        }

        @Override
        public List<McpPromptMessage> getMessages(
                final Map<String, String> arguments,
                final McpRequestContext context) {
            final McpPromptMessage message = new McpPromptMessage.Builder()
                    .setText("Hello, " + arguments.getOrDefault("name", "you") + "!")
                    .build();
            return ImmutableList.of(message);
        }
    };

    /**
     * A tool with an output schema, returning structured results.
     */
    private static final McpTool ADD = new McpTool() {
        @Override
        public String getName() {
            return "add";
        }

        @Override
        public String getDescription() {
            return "Adds two numbers.";
        }

        @Override
        public Map<String, Object> getInputProperties() {
            return ImmutableMap.of("a", ImmutableMap.of("type", "number"), "b", ImmutableMap.of("type", "number"));
        }

        @Override
        public Map<String, Object> getOutputSchema() {
            return ImmutableMap.of("type", "object",
                    "properties", ImmutableMap.of("sum", ImmutableMap.of("type", "number")),
                    "required", ImmutableList.of("sum"));
        }

        @Override
        public McpToolResult call(
                final Map<String, Object> arguments,
                final McpRequestContext context) throws Exception {
            final double a = ((Number) arguments.get("a")).doubleValue();
            final double b = ((Number) arguments.get("b")).doubleValue();

            return new McpToolResult.Builder()
                    .setStructuredContent(ImmutableMap.of("sum", a + b), MAPPER)
                    .build();
        }
    };

    private static final McpServer SERVER = new McpServer.Builder("example", "1.0")
            .setTitle("Example")
            .setInstructions(() -> "Use the tools.")
            .setTools(ImmutableList.of(ECHO, ADD))
            .setPrompts(ImmutableList.of(HELLO))
            .build();

    @Test
    public void initializeNegotiatesTheProtocolVersionTest() throws Exception {
        final JsonNode known = call("initialize",
                "{\"protocolVersion\":\"2025-06-18\",\"clientInfo\":{\"name\":\"t\"}}");
        assertEquals("2025-06-18", known.at("/result/protocolVersion").asText());
        assertEquals("example", known.at("/result/serverInfo/name").asText());
        assertEquals("Example", known.at("/result/serverInfo/title").asText());
        assertEquals("Use the tools.", known.at("/result/instructions").asText());
        assertTrue(known.at("/result/capabilities/tools").isObject());
        assertTrue(known.at("/result/capabilities/prompts").isObject());

        final JsonNode unknown = call("initialize", "{\"protocolVersion\":\"1999-01-01\"}");
        assertEquals(McpProtocolVersions.LATEST, unknown.at("/result/protocolVersion").asText());
    }

    @Test
    public void listsToolsAndPromptsTest() throws Exception {
        final JsonNode tool = call("tools/list", null).at("/result/tools/0");
        assertEquals("echo_text", tool.path("name").asText());
        assertEquals("Echo text", tool.path("title").asText());
        assertEquals("object", tool.at("/inputSchema/type").asText());
        assertEquals("string", tool.at("/inputSchema/properties/text/type").asText());
        assertEquals("text", tool.at("/inputSchema/required/0").asText());
        assertTrue(tool.at("/annotations/readOnlyHint").asBoolean());
        assertTrue(tool.at("/annotations/destructiveHint").isMissingNode());

        final JsonNode prompt = call("prompts/list", null).at("/result/prompts/0");
        assertEquals("hello", prompt.path("name").asText());
        assertTrue(prompt.path("title").isMissingNode());
        assertEquals("name", prompt.at("/arguments/0/name").asText());
        assertEquals("Who to greet.", prompt.at("/arguments/0/description").asText());
        assertTrue(prompt.at("/arguments/0/required").asBoolean());
        assertFalse(prompt.at("/arguments/1/required").asBoolean());
        assertTrue(prompt.at("/arguments/1/description").isMissingNode());
    }

    @Test
    public void callsToolsTest() throws Exception {
        final JsonNode ok = call("tools/call", "{\"name\":\"echo_text\",\"arguments\":{\"text\":\"hi\"}}");
        assertEquals("text", ok.at("/result/content/0/type").asText());
        assertEquals("hi", ok.at("/result/content/0/text").asText());
        assertFalse(ok.at("/result/isError").asBoolean());

        // A tool that throws gives an error result the model can read, not a JSON-RPC error.
        final JsonNode failed = call("tools/call", "{\"name\":\"echo_text\",\"arguments\":{\"text\":\"boom\"}}");
        assertTrue(failed.at("/result/isError").asBoolean());
        final String failure = failed.at("/result/content/0/text").asText();
        assertTrue(failure.startsWith("The tool failed: java.lang.IllegalStateException: exploded"), failure);

        final JsonNode invalid = call("tools/call", "{\"name\":\"echo_text\"}");
        assertEquals(JsonRpcError.INVALID_PARAMS, invalid.at("/error/code").asInt());
        assertTrue(invalid.path("result").isMissingNode());

        final JsonNode unknown = call("tools/call", "{\"name\":\"nope\"}");
        assertEquals("Unknown tool: nope", unknown.at("/error/message").asText());
    }

    @Test
    public void getsPromptsTest() throws Exception {
        final JsonNode prompt = call("prompts/get", "{\"name\":\"hello\",\"arguments\":{\"name\":\"Ada\"}}");
        assertEquals("Says hello.", prompt.at("/result/description").asText());
        assertEquals("user", prompt.at("/result/messages/0/role").asText());
        assertEquals("Hello, Ada!", prompt.at("/result/messages/0/content/text").asText());

        assertEquals(JsonRpcError.INVALID_PARAMS, call("prompts/get", "{\"name\":\"nope\"}").at("/error/code").asInt());

        final JsonNode missing = call("prompts/get", "{\"name\":\"hello\",\"arguments\":{\"mood\":\"sunny\"}}");
        assertEquals(JsonRpcError.INVALID_PARAMS, missing.at("/error/code").asInt());
        assertEquals("Missing required argument: name", missing.at("/error/message").asText());
    }

    @Test
    public void structuredResultsTest() throws Exception {
        final JsonNode tool = call("tools/list", null).at("/result/tools/1");
        assertEquals("add", tool.path("name").asText());
        assertEquals("number", tool.at("/outputSchema/properties/sum/type").asText());
        assertTrue(call("tools/list", null).at("/result/tools/0/outputSchema").isMissingNode());

        final JsonNode result = call("tools/call", "{\"name\":\"add\",\"arguments\":{\"a\":2,\"b\":3}}");
        assertEquals(5.0d, result.at("/result/structuredContent/sum").asDouble(), 0.0d);
        // The same result as text, for clients that don't read structured content.
        assertEquals("{\"sum\":5.0}", result.at("/result/content/0/text").asText());
        assertTrue(call("tools/call", "{\"name\":\"echo_text\",\"arguments\":{\"text\":\"hi\"}}")
                .at("/result/structuredContent").isMissingNode());
    }

    @Test
    public void protocolErrorsTest() throws Exception {
        assertEquals(JsonRpcError.METHOD_NOT_FOUND, call("resources/list", null).at("/error/code").asInt());
        assertTrue(call("ping", null).path("result").isObject());

        final JsonRpcRequest version1 = new JsonRpcRequest.Builder()
                .setJsonRpc("1.0")
                .setId(MAPPER.getNodeFactory().numberNode(1))
                .setMethod("ping")
                .build();
        final Optional<JsonRpcResponse> notJsonRpc2 = SERVER.handle(version1);
        assertEquals(JsonRpcError.INVALID_REQUEST, notJsonRpc2.orElseThrow().getError().getCode());
    }

    @Test
    public void notificationsGetNoResponseTest() {
        final JsonRpcRequest notification = new JsonRpcRequest.Builder()
                .setJsonRpc(JsonRpcResponse.VERSION)
                .setMethod("notifications/initialized")
                .build();
        assertTrue(SERVER.handle(notification).isEmpty());
    }

    @Test
    public void duplicateToolNamesFailTest() {
        assertThrows(IllegalArgumentException.class, () -> new McpServer.Builder("example", "1.0")
                .setTools(ImmutableList.of(ECHO, ECHO))
                .build());
    }

    private static JsonNode call(
            final String method,
            final String params) throws Exception {
        final JsonRpcRequest request = new JsonRpcRequest.Builder()
                .setJsonRpc(JsonRpcResponse.VERSION)
                .setId(MAPPER.getNodeFactory().numberNode(7))
                .setMethod(method)
                .setParams((params == null) ? null : MAPPER.readTree(params))
                .build();
        final JsonNode response = MAPPER.valueToTree(SERVER.handle(request).orElseThrow());
        assertEquals(7, response.path("id").asInt());
        assertEquals(JsonRpcResponse.VERSION, response.path("jsonrpc").asText());
        return response;
    }

}
