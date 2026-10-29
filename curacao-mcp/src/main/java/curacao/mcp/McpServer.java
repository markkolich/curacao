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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import curacao.mcp.exceptions.CuracaoMcpException;
import curacao.mcp.jsonrpc.JsonRpcError;
import curacao.mcp.jsonrpc.JsonRpcRequest;
import curacao.mcp.jsonrpc.JsonRpcResponse;
import curacao.mcp.prompts.McpPrompt;
import curacao.mcp.prompts.McpPromptArgument;
import curacao.mcp.tools.McpTool;
import curacao.mcp.tools.McpToolResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A Model Context Protocol server: answers the JSON-RPC methods {@code initialize},
 * {@code ping}, {@code tools/list}, {@code tools/call}, {@code prompts/list} and
 * {@code prompts/get} for the tools and prompts it's built with. Transport-independent;
 * {@link curacao.mcp.http.McpHttpTransport} serves it over Streamable HTTP.
 *
 * <p>The server is stateless: no sessions, no server-initiated requests or notifications,
 * and no resources, completions, sampling, progress or cancellation.</p>
 */
public final class McpServer {

    private static final Logger LOG = getLogger(McpServer.class);

    private static final String METHOD_INITIALIZE = "initialize";
    private static final String METHOD_PING = "ping";
    private static final String METHOD_TOOLS_LIST = "tools/list";
    private static final String METHOD_TOOLS_CALL = "tools/call";
    private static final String METHOD_PROMPTS_LIST = "prompts/list";
    private static final String METHOD_PROMPTS_GET = "prompts/get";

    private static final String NAME = "name";
    private static final String TITLE = "title";
    private static final String VERSION = "version";
    private static final String DESCRIPTION = "description";
    private static final String ARGUMENTS = "arguments";
    private static final String MESSAGES = "messages";
    private static final String TOOLS = "tools";
    private static final String PROMPTS = "prompts";
    private static final String PROTOCOL_VERSION = "protocolVersion";
    private static final String CAPABILITIES = "capabilities";
    private static final String SERVER_INFO = "serverInfo";
    private static final String CLIENT_INFO = "clientInfo";
    private static final String INSTRUCTIONS = "instructions";
    private static final String TYPE = "type";
    private static final String OBJECT = "object";
    private static final String PROPERTIES = "properties";
    private static final String REQUIRED = "required";
    private static final String INPUT_SCHEMA = "inputSchema";
    private static final String OUTPUT_SCHEMA = "outputSchema";
    private static final String ANNOTATIONS = "annotations";
    private static final String UNKNOWN_CLIENT = "unknown";

    private static final TypeReference<Map<String, Object>> TOOL_ARGUMENTS = new TypeReference<>() {};
    private static final TypeReference<Map<String, String>> PROMPT_ARGUMENTS = new TypeReference<>() {};

    private final Map<String, Object> serverInfo_;
    private final Supplier<String> instructions_;
    private final List<String> protocolVersions_;
    private final Map<String, McpTool> tools_;
    private final Map<String, McpPrompt> prompts_;
    private final ObjectMapper objectMapper_;

    private final List<Map<String, Object>> toolDefinitions_;
    private final List<Map<String, Object>> promptDefinitions_;

    private McpServer(
            final Builder builder) {
        final ImmutableMap.Builder<String, Object> serverInfo = ImmutableMap.<String, Object>builder()
                .put(NAME, builder.name_);
        if (builder.title_ != null) {
            serverInfo.put(TITLE, builder.title_);
        }
        serverInfo_ = serverInfo.put(VERSION, builder.version_).build();
        instructions_ = builder.instructions_;
        protocolVersions_ = ImmutableList.copyOf(builder.protocolVersions_);
        // Fails on a duplicate name.
        tools_ = builder.tools_.stream().collect(ImmutableMap.toImmutableMap(McpTool::getName, Function.identity()));
        prompts_ = builder.prompts_.stream()
                .collect(ImmutableMap.toImmutableMap(McpPrompt::getName, Function.identity()));
        objectMapper_ = builder.objectMapper_;

        toolDefinitions_ = tools_.values().stream()
                .map(McpServer::describe)
                .collect(ImmutableList.toImmutableList());
        promptDefinitions_ = prompts_.values().stream()
                .map(McpServer::describe)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Handles one message with no request context, as when calling the server directly.
     */
    public Optional<JsonRpcResponse> handle(
            @Nonnull final JsonRpcRequest request) {
        return handle(request, McpRequestContext.EMPTY);
    }

    /**
     * Handles one message. Empty for a notification (or a client's response to a server
     * request, which this server never makes): those get no response.
     *
     * @param context the request's context, passed to tools and prompts
     */
    public Optional<JsonRpcResponse> handle(
            @Nonnull final JsonRpcRequest request,
            @Nonnull final McpRequestContext context) {
        checkNotNull(request, "Request cannot be null.");
        checkNotNull(context, "Request context cannot be null.");

        final JsonNode id = request.getId();
        if (request.expectsNoResponse()) {
            return Optional.empty();
        } else if (!JsonRpcResponse.VERSION.equals(request.getJsonRpc())) {
            return Optional.of(error(id, JsonRpcError.INVALID_REQUEST, "Not a JSON-RPC 2.0 request."));
        }

        final JsonNode params = (request.getParams() == null)
                ? objectMapper_.createObjectNode() : request.getParams();
        try {
            final JsonRpcResponse response = new JsonRpcResponse.Builder()
                    .setId(id)
                    .setResult(dispatch(request.getMethod(), params, context))
                    .build();
            return Optional.of(response);
        } catch (final CuracaoMcpException e) {
            return Optional.of(error(id, e.getCode(), e.getMessage()));
        } catch (final Exception e) {
            LOG.warn("MCP method failed: {}", request.getMethod(), e);
            return Optional.of(error(id, JsonRpcError.INTERNAL_ERROR, "Internal error: " + e.getMessage()));
        }
    }

    private static JsonRpcResponse error(
            @Nullable final JsonNode id,
            final int code,
            final String message) {
        return new JsonRpcResponse.Builder()
                .setId(id)
                .setError(code, message)
                .build();
    }

    /**
     * Whether the server speaks a protocol version.
     */
    public boolean supportsProtocolVersion(
            @Nullable final String protocolVersion) {
        return protocolVersions_.contains(protocolVersion);
    }

    private Object dispatch(
            final String method,
            final JsonNode params,
            final McpRequestContext context) throws Exception {
        switch (method) {
            case METHOD_INITIALIZE:
                return initialize(params);
            case METHOD_PING:
                return ImmutableMap.of();
            case METHOD_TOOLS_LIST:
                return ImmutableMap.of(TOOLS, toolDefinitions_);
            case METHOD_TOOLS_CALL:
                return callTool(params, context);
            case METHOD_PROMPTS_LIST:
                return ImmutableMap.of(PROMPTS, promptDefinitions_);
            case METHOD_PROMPTS_GET:
                return getPrompt(params, context);
            default:
                throw new CuracaoMcpException(JsonRpcError.METHOD_NOT_FOUND, "Method not found: " + method);
        }
    }

    private Map<String, Object> initialize(
            final JsonNode params) {
        final String requested = params.path(PROTOCOL_VERSION).asText(null);
        // A client asking for a version this server speaks gets it; any other gets the newest,
        // and decides for itself whether to continue.
        final String version = protocolVersions_.contains(requested) ? requested : protocolVersions_.get(0);
        LOG.info("MCP client connected: {} (protocol {})", params.path(CLIENT_INFO).path(NAME).asText(UNKNOWN_CLIENT),
                version);
        final Map<String, Object> capabilities = Maps.newLinkedHashMap();
        if (!tools_.isEmpty()) {
            capabilities.put(TOOLS, ImmutableMap.of());
        }
        if (!prompts_.isEmpty()) {
            capabilities.put(PROMPTS, ImmutableMap.of());
        }
        final ImmutableMap.Builder<String, Object> result = ImmutableMap.<String, Object>builder()
                .put(PROTOCOL_VERSION, version)
                .put(CAPABILITIES, capabilities)
                .put(SERVER_INFO, serverInfo_);
        final String instructions = (instructions_ == null) ? null : instructions_.get();
        if (StringUtils.isNotBlank(instructions)) {
            result.put(INSTRUCTIONS, instructions);
        }
        return result.build();
    }

    private McpToolResult callTool(
            final JsonNode params,
            final McpRequestContext context) {
        final String name = params.path(NAME).asText(null);
        if (StringUtils.isBlank(name)) {
            throw new CuracaoMcpException(JsonRpcError.INVALID_PARAMS, "A tool call needs a name.");
        }
        final McpTool tool = tools_.get(name);
        if (tool == null) {
            throw new CuracaoMcpException(JsonRpcError.INVALID_PARAMS, "Unknown tool: " + name);
        }
        final JsonNode arguments = params.path(ARGUMENTS);
        final Map<String, Object> input = arguments.isObject()
                ? objectMapper_.convertValue(arguments, TOOL_ARGUMENTS) : ImmutableMap.of();
        McpToolResult result;
        try {
            result = checkNotNull(tool.call(input, context), "Tool returned a null result: %s", name);
        } catch (final CuracaoMcpException e) {
            throw e;
        } catch (final Exception e) {
            LOG.warn("MCP tool failed: {}", name, e);
            result = new McpToolResult.Builder()
                    .setText("The tool failed: " + ExceptionUtils.getStackTrace(e))
                    .setError(true)
                    .build();
        }
        if (tool.getOutputSchema() != null && !result.isError() && result.getStructuredContent() == null) {
            LOG.warn("MCP tool {} has an output schema but returned no structured content.", name);
        }
        LOG.debug("MCP tool call: {} (error: {})", name, result.isError());
        return result;
    }

    private Map<String, Object> getPrompt(
            final JsonNode params,
            final McpRequestContext context) throws Exception {
        final String name = params.path(NAME).asText(null);
        final McpPrompt prompt = (name == null) ? null : prompts_.get(name);
        if (prompt == null) {
            throw new CuracaoMcpException(JsonRpcError.INVALID_PARAMS, "Unknown prompt: " + name);
        }
        final JsonNode arguments = params.path(ARGUMENTS);
        final Map<String, String> input = arguments.isObject()
                ? objectMapper_.convertValue(arguments, PROMPT_ARGUMENTS) : ImmutableMap.of();
        for (final McpPromptArgument argument : prompt.getArguments()) {
            if (argument.isRequired() && StringUtils.isBlank(input.get(argument.getName()))) {
                throw new CuracaoMcpException(JsonRpcError.INVALID_PARAMS, "Missing required argument: "
                        + argument.getName());
            }
        }
        final ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        if (prompt.getDescription() != null) {
            result.put(DESCRIPTION, prompt.getDescription());
        }
        return result.put(MESSAGES, prompt.getMessages(input, context)).build();
    }

    private static Map<String, Object> describe(
            final McpTool tool) {
        final ImmutableMap.Builder<String, Object> schema = ImmutableMap.<String, Object>builder()
                .put(TYPE, OBJECT)
                .put(PROPERTIES, tool.getInputProperties());
        if (!tool.getRequiredProperties().isEmpty()) {
            schema.put(REQUIRED, tool.getRequiredProperties());
        }
        final ImmutableMap.Builder<String, Object> definition = ImmutableMap.<String, Object>builder()
                .put(NAME, tool.getName())
                .put(TITLE, tool.getTitle())
                .put(DESCRIPTION, tool.getDescription())
                .put(INPUT_SCHEMA, schema.build());
        if (tool.getOutputSchema() != null) {
            definition.put(OUTPUT_SCHEMA, tool.getOutputSchema());
        }
        if (tool.getAnnotations() != null) {
            definition.put(ANNOTATIONS, tool.getAnnotations());
        }
        return definition.build();
    }

    private static Map<String, Object> describe(
            final McpPrompt prompt) {
        final ImmutableMap.Builder<String, Object> definition = ImmutableMap.<String, Object>builder()
                .put(NAME, prompt.getName());
        if (prompt.getTitle() != null) {
            definition.put(TITLE, prompt.getTitle());
        }
        if (prompt.getDescription() != null) {
            definition.put(DESCRIPTION, prompt.getDescription());
        }
        if (!prompt.getArguments().isEmpty()) {
            definition.put(ARGUMENTS, prompt.getArguments());
        }
        return definition.build();
    }

    public static final class Builder {

        private final String name_;
        private final String version_;

        private String title_;
        private Supplier<String> instructions_;
        private List<String> protocolVersions_ = McpProtocolVersions.ALL;
        private List<McpTool> tools_ = ImmutableList.of();
        private List<McpPrompt> prompts_ = ImmutableList.of();
        private ObjectMapper objectMapper_ = new ObjectMapper();

        /**
         * A builder for a server with this name and version.
         *
         * @param name the server's name, e.g. {@code otium}
         * @param version the server's version
         */
        public Builder(
                @Nonnull final String name,
                @Nonnull final String version) {
            name_ = checkNotNull(name, "Server name cannot be null.");
            version_ = checkNotNull(version, "Server version cannot be null.");
        }

        /**
         * A human-readable name for clients to show.
         */
        public Builder setTitle(
                @Nullable final String title) {
            title_ = title;
            return this;
        }

        /**
         * How a model should use this server, sent to every client on {@code initialize}.
         */
        public Builder setInstructions(
                @Nullable final String instructions) {
            instructions_ = (instructions == null) ? null : () -> instructions;
            return this;
        }

        /**
         * Instructions rendered on each {@code initialize}, e.g. from a template.
         */
        public Builder setInstructions(
                @Nullable final Supplier<String> instructions) {
            instructions_ = instructions;
            return this;
        }

        /**
         * The protocol versions the server speaks, newest first. Defaults to
         * {@link McpProtocolVersions#ALL}.
         */
        public Builder setProtocolVersions(
                @Nonnull final List<String> protocolVersions) {
            checkArgument(!checkNotNull(protocolVersions, "Protocol versions cannot be null.").isEmpty(),
                    "At least one protocol version is required.");
            protocolVersions_ = protocolVersions;
            return this;
        }

        /**
         * The tools, listed in this order. Names must be unique.
         */
        public Builder setTools(
                @Nonnull final List<? extends McpTool> tools) {
            tools_ = ImmutableList.copyOf(checkNotNull(tools, "Tools cannot be null."));
            return this;
        }

        /**
         * The prompts, listed in this order. Names must be unique.
         */
        public Builder setPrompts(
                @Nonnull final List<? extends McpPrompt> prompts) {
            prompts_ = ImmutableList.copyOf(checkNotNull(prompts, "Prompts cannot be null."));
            return this;
        }

        /**
         * The mapper that converts call arguments. Defaults to a plain {@link ObjectMapper}.
         */
        public Builder setObjectMapper(
                @Nonnull final ObjectMapper objectMapper) {
            objectMapper_ = checkNotNull(objectMapper, "Object mapper cannot be null.");
            return this;
        }

        public McpServer build() {
            return new McpServer(this);
        }

    }

}
