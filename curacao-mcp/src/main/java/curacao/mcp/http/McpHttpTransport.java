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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import curacao.core.servlet.HttpRequest;
import curacao.entities.CuracaoEntity;
import curacao.entities.empty.StatusCodeOnlyCuracaoEntity;
import curacao.mcp.McpRequestContext;
import curacao.mcp.McpServer;
import curacao.mcp.jsonrpc.JsonRpcError;
import curacao.mcp.jsonrpc.JsonRpcRequest;
import curacao.mcp.jsonrpc.JsonRpcResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.HOST;
import static com.google.common.net.HttpHeaders.ORIGIN;
import static curacao.core.servlet.HttpStatus.SC_ACCEPTED;
import static curacao.core.servlet.HttpStatus.SC_BAD_REQUEST;
import static curacao.core.servlet.HttpStatus.SC_FORBIDDEN;
import static curacao.core.servlet.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static curacao.core.servlet.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static curacao.core.servlet.HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Serves an {@link McpServer} over MCP's Streamable HTTP transport, statelessly: each POST
 * carries one JSON-RPC message and gets a plain JSON reply (202 with no body for a
 * notification). There's no server-sent event stream and no sessions, so GET and DELETE get a
 * 405.
 *
 * <p>Every POST is checked, in order:</p>
 *
 * <ul>
 *     <li>{@code Origin}: a request from a browser page on a host that isn't allowed gets a
 *     403, as the specification requires. Requests without one (non-browser clients) pass.</li>
 *     <li>{@code Host}: a request naming a host that isn't allowed gets a 403. This is the
 *     standard defense against DNS rebinding, where a page on an attacker's domain reaches a
 *     local server under the attacker's host name.</li>
 *     <li>{@code Content-Type}: anything but {@code application/json} gets a 415. Browsers
 *     can't send that type cross-origin without a CORS preflight, which this transport never
 *     answers.</li>
 *     <li>{@code MCP-Protocol-Version}: a version the server doesn't speak gets a 400, except
 *     on {@code initialize}, where the version is still being negotiated.</li>
 * </ul>
 *
 * <p>Tools and prompts receive a {@link McpRequestContext} with the request's headers and
 * whatever the optional {@link McpRequestContextExtractor} adds.</p>
 *
 * <p>Curacao only scans an application's own boot package, so an application routes to this
 * from its own controller:</p>
 *
 * <pre>{@code
 * @Controller
 * public final class Mcp {
 *
 *     private final McpHttpTransport transport_;
 *
 *     @Injectable
 *     public Mcp(
 *             final MyMcpServer server) {
 *         transport_ = new McpHttpTransport.Builder(server.getMcpServer()).build();
 *     }
 *
 *     @Route(value = "^/mcp$", methods = POST)
 *     public CuracaoEntity post(
 *             final HttpRequest request,
 *             @RequestBody final String body) {
 *         return transport_.post(request, body);
 *     }
 *
 *     @Route(value = "^/mcp$", methods = {GET, DELETE})
 *     public CuracaoEntity unsupported() {
 *         return transport_.methodNotAllowed();
 *     }
 *
 * }
 * }</pre>
 */
public final class McpHttpTransport {

    private static final Logger LOG = getLogger(McpHttpTransport.class);

    /**
     * The {@code Origin} and {@code Host} hosts allowed by default: this machine only.
     */
    private static final Set<String> LOCAL_HOSTS = ImmutableSet.of("localhost", "127.0.0.1", "[::1]");

    private static final MediaType JSON = MediaType.create("application", "json");

    private static final String INITIALIZE = "initialize";

    /**
     * Prefixed to a {@code Host} header (a host and port) to parse it as a URL.
     */
    private static final String HTTP_SCHEME = "http://";

    private final McpServer server_;
    private final ObjectMapper objectMapper_;
    private final Set<String> allowedOriginHosts_;
    private final Set<String> allowedHosts_;
    private final McpRequestContextExtractor contextExtractor_;

    private McpHttpTransport(
            final Builder builder) {
        server_ = builder.server_;
        objectMapper_ = builder.objectMapper_;
        allowedOriginHosts_ = builder.allowedOriginHosts_;
        allowedHosts_ = builder.allowedHosts_;
        contextExtractor_ = builder.contextExtractor_;
    }

    /**
     * Handles a POST: one JSON-RPC message.
     *
     * @param request the HTTP request, for its headers
     * @param body the request body
     */
    public CuracaoEntity post(
            @Nonnull final HttpRequest request,
            @Nullable final String body) {
        checkNotNull(request, "HTTP request cannot be null.");

        final String origin = request.getHeader(ORIGIN);
        if (origin != null && !isAllowed(hostOf(origin), allowedOriginHosts_)) {
            return error(SC_FORBIDDEN, JsonRpcError.INVALID_REQUEST,
                    "This MCP server doesn't accept requests from " + origin + ".");
        }
        final String host = request.getHeader(HOST);
        if (host != null && !isAllowed(hostOf(HTTP_SCHEME + host), allowedHosts_)) {
            return error(SC_FORBIDDEN, JsonRpcError.INVALID_REQUEST,
                    "This MCP server doesn't answer to the host " + host + ".");
        }
        if (!isJson(request.getHeader(CONTENT_TYPE))) {
            return error(SC_UNSUPPORTED_MEDIA_TYPE, JsonRpcError.INVALID_REQUEST,
                    "Send JSON-RPC messages with Content-Type: application/json.");
        } else if (StringUtils.isBlank(body)) {
            return error(SC_BAD_REQUEST, JsonRpcError.INVALID_REQUEST, "A JSON-RPC request body is required.");
        }

        final JsonRpcRequest message;
        try {
            final JsonNode json = objectMapper_.readTree(body);
            if (json == null || !json.isObject()) {
                // JSON-RPC batches were dropped from MCP in protocol version 2025-06-18.
                return error(SC_BAD_REQUEST, JsonRpcError.INVALID_REQUEST,
                        "Expected one JSON-RPC message, as a JSON object.");
            }
            message = objectMapper_.treeToValue(json, JsonRpcRequest.class);
        } catch (final JsonProcessingException e) {
            return error(SC_BAD_REQUEST, JsonRpcError.PARSE_ERROR, "Invalid JSON: " + e.getOriginalMessage());
        }

        final String protocolVersion = request.getHeader(McpHttpHeaders.MCP_PROTOCOL_VERSION);
        if (protocolVersion != null && !INITIALIZE.equals(message.getMethod())
                && !server_.supportsProtocolVersion(protocolVersion)) {
            return error(SC_BAD_REQUEST, JsonRpcError.INVALID_REQUEST,
                    "Unsupported MCP protocol version: " + protocolVersion + ".");
        }

        final McpRequestContext context;
        try {
            context = context(request, protocolVersion);
        } catch (final Exception e) {
            LOG.warn("MCP request context extraction failed.", e);
            return error(SC_INTERNAL_SERVER_ERROR, JsonRpcError.INTERNAL_ERROR,
                    "The server couldn't read this request.");
        }

        final Optional<JsonRpcResponse> response = server_.handle(message, context);
        if (response.isPresent()) {
            return new JsonRpcResponseEntity.Builder(objectMapper_)
                    .setResponse(response.get())
                    .build();
        }

        return new StatusCodeOnlyCuracaoEntity(SC_ACCEPTED);
    }

    /**
     * The answer to a GET (no server-sent event stream) or a DELETE (no sessions to end).
     */
    public CuracaoEntity methodNotAllowed() {
        return error(SC_METHOD_NOT_ALLOWED, JsonRpcError.INVALID_REQUEST,
                "This MCP server answers POSTs only: it has no event stream and no sessions.");
    }

    private McpRequestContext context(
            final HttpRequest request,
            @Nullable final String protocolVersion) throws Exception {
        final McpRequestContext.Builder context = new McpRequestContext.Builder()
                .setProtocolVersion(protocolVersion);
        for (final String name : request.getHeaderNames()) {
            for (final String value : request.getHeaders(name)) {
                context.addHeader(name, value);
            }
        }
        if (contextExtractor_ != null) {
            final Map<String, Object> attributes = contextExtractor_.extract(request);
            if (attributes != null) {
                context.putAttributes(attributes);
            }
        }

        return context.build();
    }

    private JsonRpcResponseEntity error(
            final int status,
            final int code,
            final String message) {
        return new JsonRpcResponseEntity.Builder(objectMapper_)
                .setStatus(status)
                .setResponse(new JsonRpcResponse.Builder().setError(code, message).build())
                .build();
    }

    private static boolean isAllowed(
            @Nullable final String host,
            final Set<String> allowed) {
        return host != null && allowed.contains(host.toLowerCase(Locale.ROOT));
    }

    private static boolean isJson(
            @Nullable final String contentType) {
        if (contentType == null) {
            return false;
        }

        try {
            return MediaType.parse(contentType).withoutParameters().equals(JSON);
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * The host in a URL, with IPv6 addresses in brackets; null when it doesn't parse.
     */
    @Nullable
    private static String hostOf(
            final String url) {
        try {
            return URI.create(url).getHost();
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    public static final class Builder {

        private final McpServer server_;

        private ObjectMapper objectMapper_ = new ObjectMapper();
        private Set<String> allowedOriginHosts_ = LOCAL_HOSTS;
        private Set<String> allowedHosts_ = LOCAL_HOSTS;
        private McpRequestContextExtractor contextExtractor_;

        public Builder(
                @Nonnull final McpServer server) {
            server_ = checkNotNull(server, "MCP server cannot be null.");
        }

        /**
         * The mapper that reads requests and writes responses. Defaults to a plain
         * {@link ObjectMapper}.
         */
        public Builder setObjectMapper(
                @Nonnull final ObjectMapper objectMapper) {
            objectMapper_ = checkNotNull(objectMapper, "Object mapper cannot be null.");
            return this;
        }

        /**
         * The hosts an {@code Origin} header may name, lowercase, with IPv6 addresses in
         * brackets. Defaults to {@code localhost}, {@code 127.0.0.1} and {@code [::1]}.
         */
        public Builder setAllowedOriginHosts(
                @Nonnull final Set<String> allowedOriginHosts) {
            allowedOriginHosts_ = ImmutableSet.copyOf(checkNotNull(allowedOriginHosts,
                    "Allowed origin hosts cannot be null."));
            return this;
        }

        /**
         * The hosts a {@code Host} header may name, without the port, lowercase, with IPv6
         * addresses in brackets. Defaults to {@code localhost}, {@code 127.0.0.1} and
         * {@code [::1]}; a server reached by any other name (behind a proxy, say) must list it.
         */
        public Builder setAllowedHosts(
                @Nonnull final Set<String> allowedHosts) {
            allowedHosts_ = ImmutableSet.copyOf(checkNotNull(allowedHosts, "Allowed hosts cannot be null."));
            return this;
        }

        /**
         * Adds application attributes to each request's context. None by default.
         */
        public Builder setContextExtractor(
                @Nullable final McpRequestContextExtractor contextExtractor) {
            contextExtractor_ = contextExtractor;
            return this;
        }

        public McpHttpTransport build() {
            return new McpHttpTransport(this);
        }

    }

}
