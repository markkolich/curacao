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

package curacao.mcp.stdio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpServer;
import curacao.mcp.jsonrpc.JsonRpcError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class McpStdioBridgeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server_;
    private List<String> protocols_;

    /**
     * A stub MCP endpoint: echoes a request's id, accepts notifications, and fails a request
     * whose method is "fail".
     */
    @BeforeEach
    public void startServer() throws Exception {
        protocols_ = Lists.newCopyOnWriteArrayList();
        server_ = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server_.createContext("/mcp", exchange -> {
            protocols_.add(exchange.getProtocol());
            final JsonNode message = MAPPER.readTree(exchange.getRequestBody());
            final String method = message.path("method").asText();
            if (!message.has("id")) {
                exchange.sendResponseHeaders(202, -1);
            } else {
                final boolean fail = "fail".equals(method);
                final byte[] body = (fail ? "boom"
                        : "{\n  \"jsonrpc\": \"2.0\",\n  \"id\": " + message.get("id") + ",\n  \"result\": {}\n}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(fail ? 500 : 200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
            exchange.close();
        });
        server_.start();
    }

    @AfterEach
    public void stopServer() {
        server_.stop(0);
    }

    @Test
    public void forwardsMessagesTest() throws Exception {
        final List<JsonNode> replies = run(endpoint(),
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}",
                "",
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}",
                "{\"jsonrpc\":\"2.0\",\"id\":\"two\",\"method\":\"ping\"}");

        // One reply per request, none for the notification, and each on one line.
        assertEquals(2, replies.size());
        final Set<String> ids = ImmutableSet.of(replies.get(0).path("id").asText(),
                replies.get(1).path("id").asText());
        assertEquals(ImmutableSet.of("1", "two"), ids);
        assertTrue(replies.get(0).path("result").isObject());
        // Every request went out as HTTP/1.1.
        assertEquals(ImmutableSet.of("HTTP/1.1"), ImmutableSet.copyOf(protocols_));
    }

    @Test
    public void answersFailuresWithErrorsTest() throws Exception {
        final List<JsonNode> failed = run(endpoint(), "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"fail\"}");
        assertEquals(1, failed.size());
        assertEquals(3, failed.get(0).path("id").asInt());
        assertEquals(JsonRpcError.SERVER_ERROR, failed.get(0).at("/error/code").asInt());
        assertTrue(failed.get(0).at("/error/message").asText().contains("HTTP 500"));

        server_.stop(0);
        final List<JsonNode> unreachable = run(endpoint(), "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"ping\"}");
        assertEquals(4, unreachable.get(0).path("id").asInt());
        assertTrue(unreachable.get(0).at("/error/message").asText().contains("isn't reachable"));
    }

    private URI endpoint() {
        return URI.create("http://127.0.0.1:" + server_.getAddress().getPort() + "/mcp");
    }

    private static List<JsonNode> run(
            final URI endpoint,
            final String... lines) throws Exception {
        final McpStdioBridge bridge = new McpStdioBridge.Builder(endpoint)
                .setConnectTimeout(Duration.ofSeconds(2))
                .setRequestTimeout(Duration.ofSeconds(10))
                .build();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        bridge.run(new ByteArrayInputStream(String.join("\n", lines).getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8));
        final List<JsonNode> replies = Lists.newArrayList();
        for (final String line : out.toString(StandardCharsets.UTF_8).split("\n")) {
            if (!line.isEmpty()) {
                replies.add(MAPPER.readTree(line));
            }
        }
        return replies;
    }

}
