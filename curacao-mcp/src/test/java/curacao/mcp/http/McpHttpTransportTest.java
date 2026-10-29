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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import curacao.entities.CuracaoEntity;
import curacao.mcp.McpRequestContext;
import curacao.mcp.McpServer;
import curacao.mcp.jsonrpc.JsonRpcError;
import curacao.mcp.tools.McpTool;
import curacao.mcp.tools.McpToolResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class McpHttpTransportTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Echoes what it knows about the request: a header, the protocol version and an attribute.
     */
    private static final McpTool WHO_AM_I = new McpTool() {
        @Override
        public String getName() {
            return "who_am_i";
        }

        @Override
        public String getDescription() {
            return "Who's calling.";
        }

        @Override
        public Map<String, Object> getInputProperties() {
            return ImmutableMap.of();
        }

        @Override
        public McpToolResult call(
                final Map<String, Object> arguments,
                final McpRequestContext context) {
            final String text = context.getHeader("x-client") + "/" + context.getProtocolVersion() + "/"
                    + context.getAttribute("user", String.class);
            return new McpToolResult.Builder()
                    .setText(text)
                    .build();
        }
    };

    private static final McpServer SERVER = new McpServer.Builder("example", "1.0")
            .setTools(ImmutableList.of(WHO_AM_I))
            .build();

    private static final McpHttpTransport TRANSPORT = new McpHttpTransport.Builder(SERVER).build();

    private static final String PING = "{\"jsonrpc\":\"2.0\",\"id\":\"a\",\"method\":\"ping\"}";

    @Test
    public void answersRequestsTest() throws Exception {
        final CuracaoEntity entity = TRANSPORT.post(TestHttpRequest.local(), PING);
        assertEquals(200, entity.getStatus());
        assertEquals("application/json; charset=utf-8", entity.getContentType());

        final JsonNode response = render(entity);
        assertEquals("a", response.path("id").asText());
        assertTrue(response.path("result").isObject());
    }

    @Test
    public void acceptsNotificationsTest() {
        final CuracaoEntity entity = TRANSPORT.post(TestHttpRequest.local(),
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        assertEquals(202, entity.getStatus());
    }

    @Test
    public void checksTheOriginTest() throws Exception {
        assertEquals(200, post(TestHttpRequest.local().header("Origin", "http://localhost:5173")));
        assertEquals(200, post(TestHttpRequest.local().header("Origin", "http://[::1]:8080")));
        assertEquals(403, post(TestHttpRequest.local().header("Origin", "not a url")));
        assertEquals(403, post(TestHttpRequest.local().header("Origin", "null")));

        final CuracaoEntity forbidden = TRANSPORT.post(TestHttpRequest.local()
                .header("Origin", "https://evil.example"), PING);
        assertEquals(403, forbidden.getStatus());
        assertTrue(render(forbidden).path("id").isNull());

        final McpHttpTransport custom = new McpHttpTransport.Builder(SERVER)
                .setAllowedOriginHosts(ImmutableSet.of("app.example"))
                .build();
        assertEquals(200, custom.post(TestHttpRequest.local().header("Origin", "https://app.example"), PING)
                .getStatus());
        assertEquals(403, custom.post(TestHttpRequest.local().header("Origin", "http://localhost"), PING)
                .getStatus());
    }

    @Test
    public void checksTheHostTest() {
        assertEquals(200, post(TestHttpRequest.local().without("Host").header("Host", "127.0.0.1:8080")));
        assertEquals(200, post(TestHttpRequest.local().without("Host").header("Host", "[::1]:8080")));
        assertEquals(200, post(TestHttpRequest.local().without("Host").header("Host", "LOCALHOST")));
        assertEquals(200, post(TestHttpRequest.local().without("Host")));
        // DNS rebinding: the browser still names the attacker's host.
        assertEquals(403, post(TestHttpRequest.local().without("Host").header("Host", "evil.example:8080")));

        final McpHttpTransport proxied = new McpHttpTransport.Builder(SERVER)
                .setAllowedHosts(ImmutableSet.of("mcp.example"))
                .build();
        assertEquals(200, proxied.post(TestHttpRequest.local().without("Host").header("Host", "mcp.example"), PING)
                .getStatus());
        assertEquals(403, proxied.post(TestHttpRequest.local(), PING).getStatus());
    }

    @Test
    public void requiresJsonTest() throws Exception {
        assertEquals(200, post(TestHttpRequest.local().without("Content-Type")
                .header("Content-Type", "application/json; charset=utf-8")));
        assertEquals(415, post(TestHttpRequest.local().without("Content-Type")));
        assertEquals(415, post(TestHttpRequest.local().without("Content-Type").header("Content-Type", "text/plain")));
        assertEquals(415, post(TestHttpRequest.local().without("Content-Type").header("Content-Type", "garbage")));

        final CuracaoEntity form = TRANSPORT.post(TestHttpRequest.local().without("Content-Type")
                .header("Content-Type", "application/x-www-form-urlencoded"), PING);
        assertEquals(JsonRpcError.INVALID_REQUEST, render(form).at("/error/code").asInt());
    }

    @Test
    public void checksTheProtocolVersionTest() throws Exception {
        assertEquals(200, post(TestHttpRequest.local().header("MCP-Protocol-Version", "2025-06-18")));
        assertEquals(400, post(TestHttpRequest.local().header("MCP-Protocol-Version", "1999-01-01")));

        // initialize negotiates the version, so it isn't checked there.
        final String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"1999-01-01\"}}";
        final CuracaoEntity initialize = TRANSPORT.post(TestHttpRequest.local()
                .header("MCP-Protocol-Version", "1999-01-01"), body);
        assertEquals(200, initialize.getStatus());
        assertEquals("2025-11-25", render(initialize).at("/result/protocolVersion").asText());
    }

    @Test
    public void passesTheRequestContextToToolsTest() throws Exception {
        final McpHttpTransport transport = new McpHttpTransport.Builder(SERVER)
                .setContextExtractor(request -> ImmutableMap.of("user", "ada"))
                .build();
        final CuracaoEntity entity = transport.post(TestHttpRequest.local()
                .header("X-Client", "tests")
                .header("MCP-Protocol-Version", "2025-06-18"),
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"who_am_i\"}}");
        assertEquals("tests/2025-06-18/ada", render(entity).at("/result/content/0/text").asText());

        final McpHttpTransport failing = new McpHttpTransport.Builder(SERVER)
                .setContextExtractor(request -> {
                    throw new IllegalStateException("boom");
                })
                .build();
        assertEquals(500, failing.post(TestHttpRequest.local(), PING).getStatus());
    }

    @Test
    public void rejectsBadBodiesTest() throws Exception {
        final CuracaoEntity unparseable = TRANSPORT.post(TestHttpRequest.local(), "{nope");
        assertEquals(400, unparseable.getStatus());
        assertEquals(JsonRpcError.PARSE_ERROR, render(unparseable).at("/error/code").asInt());

        final CuracaoEntity batch = TRANSPORT.post(TestHttpRequest.local(), "[" + PING + "]");
        assertEquals(400, batch.getStatus());
        assertEquals(JsonRpcError.INVALID_REQUEST, render(batch).at("/error/code").asInt());

        assertEquals(400, TRANSPORT.post(TestHttpRequest.local(), " ").getStatus());
    }

    @Test
    public void writesFieldsInJsonRpcOrderTest() throws Exception {
        assertTrue(raw(TRANSPORT.post(TestHttpRequest.local(), PING))
                .startsWith("{\"jsonrpc\":\"2.0\",\"id\":\"a\",\"result\""));
        assertTrue(raw(TRANSPORT.methodNotAllowed()).startsWith("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\""));
    }

    @Test
    public void methodNotAllowedTest() throws Exception {
        final CuracaoEntity entity = TRANSPORT.methodNotAllowed();
        assertEquals(405, entity.getStatus());
        assertEquals(JsonRpcError.INVALID_REQUEST, render(entity).at("/error/code").asInt());
    }

    private static int post(
            final TestHttpRequest request) {
        return TRANSPORT.post(request, PING).getStatus();
    }

    private static JsonNode render(
            final CuracaoEntity entity) throws Exception {
        return MAPPER.readTree(raw(entity));
    }

    private static String raw(
            final CuracaoEntity entity) throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        entity.write(os);

        return os.toString(StandardCharsets.UTF_8);
    }

}
