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
import com.fasterxml.jackson.databind.node.ObjectNode;
import curacao.mcp.jsonrpc.JsonRpcError;
import curacao.mcp.jsonrpc.JsonRpcResponse;
import org.apache.commons.lang3.exception.ExceptionUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static curacao.core.servlet.HttpStatus.SC_ACCEPTED;
import static curacao.core.servlet.HttpStatus.SC_OK;

/**
 * Connects an MCP client that only launches stdio servers (Claude Desktop, say) to an MCP
 * server running over HTTP: reads JSON-RPC messages from stdin, one per line, POSTs each to the
 * endpoint, and writes each reply to stdout as one line. Run it from the application's jar:
 *
 * <pre>{@code
 * java -cp app.jar curacao.mcp.stdio.McpStdioBridge http://127.0.0.1:8080/app/mcp
 * }</pre>
 *
 * <p>{@code --connect-timeout} and {@code --request-timeout} (in seconds) override the defaults;
 * {@code --help} lists the options.</p>
 *
 * <p>Like the MCP Java SDK's {@code HttpClientStreamableHttpTransport}, it uses the JDK's
 * {@link HttpClient}, pinned to HTTP/1.1 with a connect timeout, and sends each request
 * asynchronously, so a long tool call doesn't hold up a ping.</p>
 *
 * <p>stdout carries only protocol messages, so everything else, including anything the
 * application's own code prints, goes to stderr.</p>
 */
public final class McpStdioBridge {

    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    /**
     * Some tool calls take a while.
     */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(5);

    private static final String APPLICATION_JSON = "application/json";
    /**
     * What the Streamable HTTP transport requires clients to accept.
     */
    private static final String ACCEPT_TYPES = "application/json, text/event-stream";

    private static final String JSONRPC = "jsonrpc";
    private static final String ID = "id";
    private static final String ERROR = "error";
    private static final String CODE = "code";
    private static final String MESSAGE = "message";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final URI endpoint_;
    private final Duration requestTimeout_;
    private final HttpClient httpClient_;

    private McpStdioBridge(
            final Builder builder) {
        endpoint_ = builder.endpoint_;
        requestTimeout_ = builder.requestTimeout_;
        // HTTP/1.1, as the MCP Java SDK does: no HTTP/2 upgrade attempt on a plain http:// endpoint.
        httpClient_ = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(builder.connectTimeout_)
                .build();
    }

    public static void main(
            final String... args) throws Exception {
        final PrintStream stdout = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        System.setOut(System.err);

        final Arguments arguments = new Arguments();
        final CommandLine commandLine = new CommandLine(arguments);
        try {
            commandLine.parseArgs(args);
        } catch (final CommandLine.ParameterException e) {
            System.err.println(e.getMessage());
            commandLine.usage(System.err);
            System.exit(2);
        }
        if (commandLine.isUsageHelpRequested()) {
            // To stderr, like everything that isn't a protocol message.
            commandLine.usage(System.err);
            return;
        }

        final McpStdioBridge bridge = new Builder(arguments.endpoint_)
                .setConnectTimeout(Duration.ofSeconds(arguments.connectTimeoutSeconds_))
                .setRequestTimeout(Duration.ofSeconds(arguments.requestTimeoutSeconds_))
                .build();
        System.err.println("MCP bridge: forwarding to " + bridge.endpoint_);
        bridge.run(System.in, stdout);
    }

    /**
     * Forwards every message read from {@code in} until it ends, writing replies to {@code out},
     * then waits up to the request timeout for the replies still in flight.
     */
    public void run(
            @Nonnull final InputStream in,
            @Nonnull final PrintStream out) throws Exception {
        checkNotNull(in, "Input stream cannot be null.");
        checkNotNull(out, "Output stream cannot be null.");

        final Set<CompletableFuture<Void>> inFlight = ConcurrentHashMap.newKeySet();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.isBlank()) {
                    final CompletableFuture<Void> forwarded = forward(line, out);
                    inFlight.add(forwarded);
                    forwarded.whenComplete((ignored, e) -> inFlight.remove(forwarded));
                }
                line = reader.readLine();
            }
        }
        try {
            CompletableFuture.allOf(inFlight.toArray(new CompletableFuture<?>[0]))
                    .get(requestTimeout_.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            System.err.println("MCP bridge: gave up waiting for " + inFlight.size() + " replies.");
        }
    }

    private CompletableFuture<Void> forward(
            final String message,
            final PrintStream out) {
        final HttpRequest request = HttpRequest.newBuilder(endpoint_)
                .timeout(requestTimeout_)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .header(ACCEPT, ACCEPT_TYPES)
                .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
                .build();
        return httpClient_.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(response -> reply(message, response, out))
                .exceptionally(e -> {
                    final Throwable cause = (e instanceof CompletionException && e.getCause() != null)
                            ? e.getCause() : e;
                    // The full trace goes to stderr only; the reply carries a one-line reason.
                    System.err.println("MCP bridge: request failed: " + ExceptionUtils.getStackTrace(cause));
                    fail(message, "The MCP server isn't reachable at " + endpoint_ + "; start it and try again. ("
                            + cause + ")", out);
                    return null;
                });
    }

    private void reply(
            final String message,
            final HttpResponse<String> response,
            final PrintStream out) {
        if (response.statusCode() == SC_OK) {
            try {
                // Re-serialized, so the reply is exactly one line.
                write(MAPPER.readTree(response.body()).toString(), out);
            } catch (final Exception e) {
                fail(message, "The MCP server sent a reply that isn't JSON: " + response.body(), out);
            }
        } else if (response.statusCode() != SC_ACCEPTED) {
            fail(message, "The MCP server answered HTTP " + response.statusCode() + ": " + response.body(), out);
        }
    }

    /**
     * Logs a failure, and answers the message with a JSON-RPC error if it was a request.
     */
    private static void fail(
            final String message,
            final String reason,
            final PrintStream out) {
        System.err.println("MCP bridge: " + reason);
        try {
            final JsonNode id = MAPPER.readTree(message).get(ID);
            if (id != null && !id.isNull()) {
                final ObjectNode reply = MAPPER.createObjectNode().put(JSONRPC, JsonRpcResponse.VERSION);
                reply.set(ID, id);
                reply.putObject(ERROR).put(CODE, JsonRpcError.SERVER_ERROR).put(MESSAGE, reason);
                write(reply.toString(), out);
            }
        } catch (final Exception e) {
            System.err.println("MCP bridge: unreadable message: " + message);
        }
    }

    private static void write(
            final String line,
            final PrintStream out) {
        synchronized (out) {
            out.println(line);
        }
    }

    /**
     * The bridge's command line, parsed by picocli.
     */
    @Command(name = "McpStdioBridge", description = "Connects a stdio MCP client to an MCP server over HTTP.")
    private static final class Arguments {

        @Parameters(index = "0", paramLabel = "ENDPOINT",
                description = "The MCP server's Streamable HTTP endpoint, e.g. http://127.0.0.1:8080/app/mcp.")
        private URI endpoint_;

        @Option(names = {"--connect-timeout"}, paramLabel = "SECONDS",
                description = "How long to wait for a connection, in seconds.", defaultValue = "5")
        private long connectTimeoutSeconds_;

        @Option(names = {"--request-timeout"}, paramLabel = "SECONDS",
                description = "How long to wait for a reply, in seconds.", defaultValue = "300")
        private long requestTimeoutSeconds_;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help.")
        private boolean help_;

    }

    public static final class Builder {

        private final URI endpoint_;

        private Duration connectTimeout_ = DEFAULT_CONNECT_TIMEOUT;
        private Duration requestTimeout_ = DEFAULT_REQUEST_TIMEOUT;

        /**
         * A builder for a bridge to this MCP endpoint.
         *
         * @param endpoint the MCP server's Streamable HTTP endpoint
         */
        public Builder(
                @Nonnull final URI endpoint) {
            endpoint_ = checkNotNull(endpoint, "Endpoint cannot be null.");
        }

        public Builder setConnectTimeout(
                @Nonnull final Duration connectTimeout) {
            connectTimeout_ = checkNotNull(connectTimeout, "Connect timeout cannot be null.");

            return this;
        }

        public Builder setRequestTimeout(
                @Nonnull final Duration requestTimeout) {
            requestTimeout_ = checkNotNull(requestTimeout, "Request timeout cannot be null.");

            return this;
        }

        public McpStdioBridge build() {
            return new McpStdioBridge(this);
        }

    }

}
