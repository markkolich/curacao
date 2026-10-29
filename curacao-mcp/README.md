# curacao-mcp

A small [Model Context Protocol](https://modelcontextprotocol.io) server for Curacao applications. You
describe your tools and prompts in plain Java, and curacao-mcp answers the protocol over Streamable HTTP,
using Curacao's own routing and dependency injection. A stdio bridge is included for clients that can only
launch a subprocess, such as Claude Desktop.

curacao-mcp is synchronous and has no dependencies beyond Curacao, Jackson and picocli. It doesn't use Project
Reactor or run a servlet of its own. Like the rest of Curacao, it requires Java 11 or later.

## What's supported

- The protocol methods `initialize` (with version negotiation), `ping`, `tools/list`, `tools/call`,
  `prompts/list` and `prompts/get`.
- Structured tool output (`outputSchema` and `structuredContent`), and prompt arguments.
- Protocol versions `2025-11-25`, `2025-06-18`, `2025-03-26` and `2024-11-05` (`McpProtocolVersions`).
- Stateless Streamable HTTP: one JSON-RPC message per POST, answered with plain JSON, with the spec's
  `Origin` and `MCP-Protocol-Version` checks plus `Host` and `Content-Type` checks.
- A request context for every tool call and prompt: the request's headers, and whatever your application
  attaches.

Not supported: resources, completions, sampling, elicitation, server-sent event streams, sessions
(`MCP-Session-Id`), progress, cancellation, and JSON-RPC batches.

## Dependency

```xml
<dependency>
    <groupId>curacao</groupId>
    <artifactId>curacao-mcp</artifactId>
    <version>${curacao.version}</version>
</dependency>
```

## Quick start

Exposing an MCP server takes four pieces: a tool, an optional prompt, a component that builds the
`McpServer`, and a controller that routes requests to it.

### 1. A tool

A tool implements `McpTool`. Make it a `@Component`, so Curacao can inject whatever it needs. This one
declares an output schema and returns a structured result.

```java
@Component
public final class GetTimeTool implements McpTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getName() {
        return "get_time";
    }

    @Override
    public String getDescription() {
        return "The current date and time in a time zone. Use it whenever the user asks what time it is.";
    }

    @Override
    public Map<String, Object> getInputProperties() {
        return ImmutableMap.of(
                "time_zone", ImmutableMap.of(
                        "type", "string",
                        "description", "An IANA time zone, e.g. America/Los_Angeles."));
    }

    @Override
    public List<String> getRequiredProperties() {
        return ImmutableList.of("time_zone");
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return ImmutableMap.of(
                "type", "object",
                "properties", ImmutableMap.of(
                        "time_zone", ImmutableMap.of("type", "string"),
                        "date_time", ImmutableMap.of("type", "string")),
                "required", ImmutableList.of("time_zone", "date_time"));
    }

    @Override
    public McpToolAnnotations getAnnotations() {
        return new McpToolAnnotations.Builder()
                .setReadOnlyHint(true)
                .setOpenWorldHint(false)
                .build();
    }

    @Override
    public McpToolResult call(
            final Map<String, Object> arguments,
            final McpRequestContext context) throws Exception {
        final Object timeZone = arguments.get("time_zone");
        if (!(timeZone instanceof String)) {
            // A protocol error: the call itself is malformed.
            throw new McpException(JsonRpcError.INVALID_PARAMS, "time_zone is required.");
        }

        final ZoneId zone;
        try {
            zone = ZoneId.of((String) timeZone);
        } catch (final DateTimeException e) {
            // A tool error: the model reads this and can try again with a valid zone.
            return new McpToolResult.Builder()
                    .setText("Unknown time zone: " + timeZone)
                    .setError(true)
                    .build();
        }

        // Structured, and the same JSON as text for clients that don't read structured content.
        final Map<String, String> now = ImmutableMap.of(
                "time_zone", zone.getId(),
                "date_time", ZonedDateTime.now(zone).toString());
        return new McpToolResult.Builder()
                .setStructuredContent(now, MAPPER)
                .build();
    }

}
```

Arguments arrive as plain JSON values: `String`, `Number`, `Boolean`, `List` and `Map`. The map is empty when
a call has none. The `context` describes the request that made the call; see [Request context](#request-context).
A tool without an output schema sets only the text: `new McpToolResult.Builder().setText(...).build()`.

### 2. A prompt (optional)

Prompts are starter messages that clients offer to the user, often as slash commands.

```java
@Component
public final class MorningBriefPrompt implements McpPrompt {

    @Override
    public String getName() {
        return "morning_brief";
    }

    @Override
    public String getTitle() {
        return "Morning brief";
    }

    @Override
    public String getDescription() {
        return "What's on today, in the user's time zone.";
    }

    @Override
    public List<McpPromptArgument> getArguments() {
        final McpPromptArgument timeZone = new McpPromptArgument.Builder()
                .setName("time_zone")
                .setDescription("An IANA time zone, e.g. Europe/Paris; defaults to America/Los_Angeles.")
                .build();
        return ImmutableList.of(timeZone);
    }

    @Override
    public List<McpPromptMessage> getMessages(
            final Map<String, String> arguments,
            final McpRequestContext context) {
        final String zone = arguments.getOrDefault("time_zone", "America/Los_Angeles");

        final McpPromptMessage message = new McpPromptMessage.Builder()
                .setText("Check the time in " + zone + ", then give me a short brief for the rest of my day.")
                .build();
        return ImmutableList.of(message);
    }

}
```

`prompts/list` advertises the arguments, so clients can ask the user for them. Declare one with
`setRequired(true)` and the server answers a `prompts/get` without it with a `-32602` error, so
`getMessages()` never sees it missing.

### 3. The server

Build one `McpServer` in a component and share it.

```java
@Component
public final class ExampleMcpServer {

    private final McpServer server_;

    @Injectable
    public ExampleMcpServer(
            final GetTimeTool getTimeTool,
            final MorningBriefPrompt morningBriefPrompt) {
        server_ = new McpServer.Builder("example", "1.0.0")
                .setTitle("Example")
                .setInstructions("Use get_time for anything that depends on the current date or time.")
                .setTools(ImmutableList.of(getTimeTool))
                .setPrompts(ImmutableList.of(morningBriefPrompt))
                .build();
    }

    public McpServer getServer() {
        return server_;
    }

}
```

### 4. The controller

Curacao only scans your application's boot package, so the route lives in your own controller. It hands the
request (for its headers) and the raw body to an `McpHttpTransport`. Curacao's built-in mappers supply both,
so you don't need a request mapper.

```java
@Controller
public final class Mcp {

    private final McpHttpTransport transport_;

    @Injectable
    public Mcp(
            final ExampleMcpServer exampleMcpServer) {
        transport_ = new McpHttpTransport.Builder(exampleMcpServer.getServer()).build();
    }

    @Route(value = "^/mcp$", methods = POST)
    public CuracaoEntity post(
            final HttpRequest request,
            @RequestBody final String body) {
        return transport_.post(request, body);
    }

    @Route(value = "^/mcp$", methods = {GET, DELETE})
    public CuracaoEntity unsupported() {
        return transport_.methodNotAllowed();
    }

}
```

With a context path of `/app`, the endpoint is `http://127.0.0.1:8080/app/mcp`.

## Connecting clients

**Claude Code** connects over HTTP:

```bash
claude mcp add --transport http example http://127.0.0.1:8080/app/mcp
```

**Claude Desktop** launches MCP servers as subprocesses, so point it at the stdio bridge in your application's
jar. Edit `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "example": {
      "command": "/path/to/java",
      "args": ["-cp", "/path/to/app.jar", "curacao.mcp.stdio.McpStdioBridge",
               "http://127.0.0.1:8080/app/mcp"]
    }
  }
}
```

Your application has to be running for either client to reach it.

**curl** is handy for checking the endpoint by hand:

```bash
curl -s http://127.0.0.1:8080/app/mcp -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

curl -s http://127.0.0.1:8080/app/mcp -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"get_time","arguments":{"time_zone":"Europe/Paris"}}}'
```

## Tools in detail

| Method | Required | Notes |
|---|---|---|
| `getName()` | yes | Unique within the server; building a server with two tools of the same name fails |
| `getDescription()` | yes | The model reads this to decide when to call the tool, so say what it's for |
| `getInputProperties()` | yes | JSON Schema per property; the server wraps them in `{"type": "object", "properties": ...}` |
| `getRequiredProperties()` | no | Defaults to none |
| `getTitle()` | no | Defaults to the name with spaces, capitalized: `get_time` → "Get time" |
| `getOutputSchema()` | no | The full JSON Schema (`"type": "object"`) of the tool's structured results; defaults to none |
| `getAnnotations()` | no | `readOnlyHint`, `destructiveHint`, `idempotentHint`, `openWorldHint`; unset hints are omitted |
| `call(arguments, context)` | yes | Returns an `McpToolResult` |

A tool builds its result with `McpToolResult.Builder`, which needs text:

| Builder method | Use it for |
|---|---|
| `setText(text)` | The result as text for the model. A tool without an output schema sets only this |
| `setStructuredContent(object, objectMapper)` | A tool with an output schema. `object` is anything Jackson writes as a JSON object (a map, a bean, an `ObjectNode`); the mapper also writes it as the text, which the spec says a structured result should include for older clients |
| `setStructuredContent(object)` | The same, with the text set yourself through `setText` |
| `setError(true)` | A failure the model should read; the text is the message |

curacao-mcp doesn't validate structured results against the schema. It logs a warning when a tool with an
output schema returns a successful result without structured content.

A tool can report a failure in three ways:

| What happens in `call()` | What the client gets |
|---|---|
| `return new McpToolResult.Builder().setText("...").setError(true).build()` | A result with `isError: true`, which the model reads and can act on. Use this for failures inside the tool |
| `throw new McpException(code, "...")` | A JSON-RPC error with that code, usually `JsonRpcError.INVALID_PARAMS`. Use this when the call itself is malformed |
| Any other exception | Logged at WARN, and returned as an error result: "The tool failed: <stack trace>" |

Annotations are hints that clients may use to decide whether to ask before a call. They never enforce
anything.

## Prompts in detail

| Method | Required | Notes |
|---|---|---|
| `getName()` | yes | Unique within the server |
| `getTitle()`, `getDescription()` | no | Shown by clients; default to none |
| `getArguments()` | no | Built with `McpPromptArgument.Builder` (`setName`, `setDescription`, `setRequired`); listed in `prompts/list` |
| `getMessages(arguments, context)` | yes | Built with `McpPromptMessage.Builder` (`setText`, and `setRole(Role.ASSISTANT)` for the assistant's; the user's by default) |

## Request context

Every tool call and prompt gets an `McpRequestContext` describing the request:

| Method | Returns |
|---|---|
| `getHeader(name)`, `getHeaders(name)` | The request's headers; names are case-insensitive |
| `getProtocolVersion()` | The client's `MCP-Protocol-Version` header, or null |
| `getAttribute(name, type)`, `getAttributes()` | What your application attached |

To attach your own data, give the transport an `McpRequestContextExtractor`. It runs for every POST and
returns attributes, such as the user a bearer token belongs to:

```java
transport_ = new McpHttpTransport.Builder(server)
        .setContextExtractor(request -> {
            final String authorization = request.getHeader(AUTHORIZATION);
            final User user = tokens.userFor(authorization);
            return (user == null) ? ImmutableMap.of() : ImmutableMap.of("user", user);
        })
        .build();
```

Then a tool reads it with `context.getAttribute("user", User.class)`. `HttpRequest.getDelegate()` is the
servlet request, for anything the headers don't carry, such as the remote address.

An extractor only collects data. To turn a request away, for instance one without valid credentials, use a
Curacao request filter or check in your controller before calling the transport. If the extractor throws,
the transport answers with an HTTP 500. When you call `McpServer.handle()` directly, tools and prompts get
`McpRequestContext.EMPTY`.

## Server options

| `McpServer.Builder` | Default | Notes |
|---|---|---|
| `new Builder(name, version)` | | Sent as `serverInfo` on `initialize` |
| `setTitle(String)` | none | A display name |
| `setInstructions(String)` | none | Tells the model how to use the server; sent on every `initialize` |
| `setInstructions(Supplier<String>)` | none | Re-rendered on each `initialize`, e.g. from a template |
| `setTools(List)` / `setPrompts(List)` | none | Listed in this order. The server advertises the `tools` and `prompts` capabilities only when there are some |
| `setProtocolVersions(List)` | `McpProtocolVersions.ALL` | Newest first. A client asking for a listed version gets it; any other gets the newest |
| `setObjectMapper(ObjectMapper)` | `new ObjectMapper()` | Converts call arguments |

`McpServer.handle(JsonRpcRequest, McpRequestContext)` doesn't depend on HTTP. It returns the response, or
nothing for a notification, so you can serve it over another transport or call it directly in tests.

## HTTP transport

Every POST is checked in this order:

| Request | Response |
|---|---|
| An `Origin` header naming a host that isn't allowed | 403 with a `-32600` error |
| A `Host` header naming a host that isn't allowed | 403 with a `-32600` error |
| A `Content-Type` other than `application/json` (parameters such as `charset` are fine) | 415 with a `-32600` error |
| An empty body, a batch or a non-object | 400 with a `-32600` error |
| Invalid JSON | 400 with a `-32700` parse error |
| An `MCP-Protocol-Version` the server doesn't speak (not checked on `initialize`) | 400 with a `-32600` error |
| A request (has an `id`) | 200 with the JSON-RPC response |
| A notification (no `id`) | 202 with no body |

GET and DELETE, routed to `methodNotAllowed()`, get a 405: there's no event stream and no sessions. The
header names are constants in `McpHttpHeaders`.

### What the checks protect against

The first three stop a web page from using a visitor's browser against your server. They don't stop
anything else: curl, a script or a local process can send whatever headers it likes.

- **`Origin`** is required by the MCP spec. A browser sets it on every cross-site request, and page
  scripts can't change it. Requests without one (Claude Code, the bridge, curl) are allowed; `Origin: null`
  (sandboxed frames) is not.
- **`Host`** is the standard defense against DNS rebinding, where the attacker's domain is made to resolve to
  127.0.0.1 so the page can read your server's replies. The browser still sends the attacker's host name,
  which the check rejects.
- **`Content-Type: application/json`** can't be sent cross-origin without a CORS preflight, which this
  transport never answers. That shuts out form posts and `text/plain` requests, which browsers send without
  asking.

By default only `localhost`, `127.0.0.1` and `[::1]` are allowed, for both `Origin` and `Host`. A server
reached by another name, behind a proxy for instance, must list it:

```java
transport_ = new McpHttpTransport.Builder(server)
        .setAllowedHosts(ImmutableSet.of("localhost", "mcp.example.com"))
        .setAllowedOriginHosts(ImmutableSet.of("localhost", "app.example.com"))
        .setObjectMapper(objectMapper)
        .build();
```

The transport doesn't authenticate anyone. Bind your application to 127.0.0.1, which the spec recommends for
local servers, and put anything else behind your own authentication (a request filter, with the
[request context](#request-context) carrying the caller to your tools).

## Stdio bridge

`curacao.mcp.stdio.McpStdioBridge` reads JSON-RPC messages from stdin, one per line, POSTs each to the
endpoint, and writes each reply to stdout as a single line. Requests go out concurrently, so a slow tool call
doesn't hold up a ping. It uses the JDK's `java.net.http.HttpClient`, pinned to HTTP/1.1 with a connect
timeout, the same way the MCP Java SDK's `HttpClientStreamableHttpTransport` does.

```
Usage: McpStdioBridge [-h] [--connect-timeout=SECONDS]
                      [--request-timeout=SECONDS] ENDPOINT
      ENDPOINT   The MCP server's Streamable HTTP endpoint, e.g. http://127.
                   0.0.1:8080/app/mcp.
      --connect-timeout=SECONDS
                 How long to wait for a connection, in seconds.
  -h, --help     Show this help.
      --request-timeout=SECONDS
                 How long to wait for a reply, in seconds.
```

The defaults are 5 seconds to connect and 300 seconds for a reply. stdout carries only protocol messages;
everything else, including help and errors, goes to stderr. When the server can't be reached or answers with
an HTTP error, each request gets a JSON-RPC error (code `-32000`) explaining why.

To bake in a default endpoint, write your own `main` and call the bridge directly:

```java
public static void main(
        final String... args) throws Exception {
    final PrintStream stdout = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    System.setOut(System.err);

    new McpStdioBridge.Builder(URI.create("http://127.0.0.1:8080/app/mcp"))
            .setRequestTimeout(Duration.ofMinutes(10))
            .build()
            .run(System.in, stdout);
}
```

## Testing

To test tools and prompts, build an `McpServer` and call `handle()` with a `JsonRpcRequest`. You don't need
an HTTP server. `McpServerTest` and `McpHttpTransportTest` in this module show the pattern.

```java
final JsonRpcRequest request = new JsonRpcRequest.Builder()
        .setJsonRpc(JsonRpcResponse.VERSION)
        .setId(mapper.getNodeFactory().numberNode(1))
        .setMethod("tools/call")
        .setParams(mapper.readTree("{\"name\":\"get_time\",\"arguments\":{\"time_zone\":\"UTC\"}}"))
        .build();
final JsonRpcResponse response = server.handle(request).orElseThrow();
```

To test a tool that reads the context, build one:

```java
final McpRequestContext context = new McpRequestContext.Builder()
        .addHeader("Authorization", "Bearer test")
        .putAttribute("user", testUser)
        .build();
final JsonRpcResponse response = server.handle(request, context).orElseThrow();
```
