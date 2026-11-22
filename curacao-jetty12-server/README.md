# curacao-jetty12-server

Builds an embedded [Jetty 12](https://jetty.org) server that hosts a Curacao application, so an application's
`main()` doesn't have to assemble the thread pool, connector, servlet context and Curacao dispatcher by
hand. Unlike the rest of Curacao, which needs Java 11 or later, this module requires **Java 17 or
later**: Jetty 12 is compiled for Java 17, so a Java 11 JVM throws `UnsupportedClassVersionError`
on the first Jetty class it loads. Stay on `curacao-jetty11-server` if you need Java 11.

`CuracaoJettyServer.Builder` builds a Jetty `Server` but doesn't start it. Your `main()` starts it, and tests
can boot the whole application on a free port.

This is the Jetty 12 counterpart of `curacao-jetty11-server`, with the same builder API. Jetty 11 is
[end of life](https://webtide.com/end-of-life-changes-to-eclipse-jetty-and-cometd/) — as of 1 January 2026
the Jetty project no longer publishes updates for it to Maven Central — so new applications should start
here, and existing ones should move. See [Moving from Jetty 11](#moving-from-jetty-11) for what changes.

## What you get

- A `WebAppContext` under your context path, with no sessions, no security handler and no JSP engine.
- Curacao's context listener and `CuracaoJakartaDispatcherServlet` at `/*`, initialized when the server
  starts.
- Any servlets you add beside it, such as one for static files (see
  [Static files and other servlets](#static-files-and-other-servlets)).
- No `Server` or `X-Powered-By` response headers.
- A connector on `127.0.0.1:8080` with a 30-second idle timeout.
- The base resource found for you: `src/main/webapp` when running from source, otherwise a `webapp`
  directory on the classpath (where a fat JAR packages it). It's also stored in the context attribute
  `CuracaoJettyServer.BASE_RESOURCE_CONTEXT_ATTRIBUTE`, for components that read templates or files at
  runtime.
- A JVM shutdown hook that stops the server.

## Dependency

```xml
<dependency>
    <groupId>curacao</groupId>
    <artifactId>curacao-jetty12-server</artifactId>
    <version>${curacao.version}</version>
</dependency>
```

It brings in `curacao`, `curacao-servlet-jakarta`, Jetty 12's `jetty-server`, and `jetty-ee11-servlet` and
`jetty-ee11-webapp` from Jetty's EE11 environment.

### Why EE11

Jetty 12 moved the servlet container out of core and into one module per Servlet specification, so the same
core server can host several. EE11 implements Servlet 6.1, which is the `jakarta.servlet-api` version
`curacao-servlet-jakarta` is built against, so that's the one this module uses. The practical consequence is
that servlet classes live under `org.eclipse.jetty.ee11.*` rather than `org.eclipse.jetty.*`, which reaches
you wherever you hand the builder a `ServletHolder`.

## Quick start

```java
public final class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Option(names = {"--host"}, paramLabel = "HOST", description = "Server host.",
            defaultValue = "127.0.0.1")
    private String host_;

    @Option(names = {"--port"}, paramLabel = "PORT", description = "Server port.",
            defaultValue = "8080")
    private int port_;

    public static void main(
            final String... args) {
        try {
            final Application app = new Application();
            new CommandLine(app).parseArgs(args);

            final Server server = new CuracaoJettyServer.Builder("/myapp")
                    .setHost(app.host_)
                    .setPort(app.port_)
                    .addServlet(staticFiles(), "/static/*")
                    .build();
            server.start();
            server.join();
        } catch (final Exception e) {
            LOG.error("Startup failed.", e);
        }
    }

    private static ServletHolder staticFiles() {
        final ServletHolder holder = new ServletHolder(ResourceServlet.class);
        holder.setAsyncSupported(true);
        holder.setInitParameter("dirAllowed", "false");
        holder.setInitParameter("pathInfoOnly", "false");
        return holder;
    }

}
```

As always, Curacao finds your controllers, components and mappers in the `curacao.boot-package` from your
`application.conf`:

```hocon
curacao {
  boot-package = "com.example.myapp"
}
```

With that, `GET http://127.0.0.1:8080/myapp/...` goes to your controllers, and
`/myapp/static/app.css` is served from `src/main/webapp/static/app.css`.

## Options

| Method | Default | What it does |
|---|---|---|
| `new Builder(contextPath)` | (required) | The context path, such as `"/myapp"`, or `"/"` for the root. Must start with `/`. |
| `setThreadNameFormat(String)` | `jetty-%d` | The names of the server's threads, a `String.format` format given each thread's number, as in Guava's `ThreadFactoryBuilder.setNameFormat`: `"onyx-%d"` names them `onyx-0`, `onyx-1` and so on. |
| `setHost(String)` | `127.0.0.1` | The address to listen on. Use `0.0.0.0` to listen on every interface. |
| `setPort(int)` | `8080` | The port. `0` picks a free one (see [Testing](#testing)). |
| `setIdleTimeout(Duration)` | 30 seconds | How long a connection may sit with no bytes flowing before Jetty closes it. Raise it for slow responses, such as long-running API calls. |
| `customizeConnector(Consumer<ServerConnector>)` | | Changes the default connector after the host, port and idle timeout are set (see [Connectors](#connectors)). |
| `setConnectorFactory(Function<Server, Connector>)` | | Replaces the default connector with your own (see [Connectors](#connectors)). |
| `setBaseResource(Resource)` | found for you | The context's "webapp" directory, when the default lookup doesn't fit. Built through a `ResourceFactory` (see [Base resources](#base-resources)). |
| `setContextAttribute(name, value)` | | Adds a servlet context attribute. |
| `setDispatcherServlet(Class)` | `CuracaoJakartaDispatcherServlet` | Your own subclass of the Curacao dispatcher. |
| `setCuracaoMapping(String)` | `/*` | Where the Curacao dispatcher is mapped. |
| `addServlet(ServletHolder, pathSpec)` | none | Adds a servlet you configured, such as one for static files (see [Static files and other servlets](#static-files-and-other-servlets)). |
| `setErrorPage(String)` | none | A path that every container error is forwarded to (see [Error pages](#error-pages)). |
| `setFailOnStartupError(boolean)` | `false` | Whether a failed boot makes `server.start()` throw (see [When the application fails to boot](#when-the-application-fails-to-boot)). |
| `setStopAtShutdown(boolean)` | `true` | Whether a JVM shutdown hook stops the server. |
| `customizeContext(Consumer<WebAppContext>)` | | Changes the context after the builder has configured it. |
| `customizeServer(Consumer<Server>)` | | Changes the server after the builder has configured it. |

A fuller example:

```java
final ServletHolder staticFiles = new ServletHolder(ResourceServlet.class);
staticFiles.setAsyncSupported(true);
staticFiles.setInitParameter("dirAllowed", "false");
staticFiles.setInitParameter("pathInfoOnly", "false");
staticFiles.setInitParameter("cacheControl", "public,max-age=3600");
staticFiles.setInitParameter("acceptRanges", "true");

final Server server = new CuracaoJettyServer.Builder("/onyx")
        .setHost(host)
        .setPort(port)
        .addServlet(staticFiles, "/static/*")
        .setErrorPage("/forward-errors")
        .build();
```

## Static files and other servlets

The builder only sets up Curacao. Anything else you want in the context, you configure as a Jetty
`ServletHolder` (`org.eclipse.jetty.ee11.servlet.ServletHolder`) and add with `addServlet(holder, pathSpec)`.
Servlets are added in the order given, and Curacao, at `/*`, answers every path that no servlet claims more
specifically.

For static files, add Jetty's **`ResourceServlet`**, not `DefaultServlet`. Jetty 12.1 split the two:
`DefaultServlet` is now only for the container's default `/` mapping, and mapped anywhere else it logs
`Incorrect mapping for DefaultServlet at /static/*. Use ResourceServlet` and then resolves paths as
`ResourceServlet` would anyway. `ResourceServlet` is the one meant for a prefix mapping.

Two init parameters need setting deliberately:

- **`dirAllowed=false`** — it lists a directory's contents by default, as `DefaultServlet` did.
- **`pathInfoOnly=false`** — this one is new, and it is the easiest thing to get wrong. `ResourceServlet`
  defaults it to `true`, where Jetty 11's `DefaultServlet` defaulted it to `false`. Left alone, a servlet
  mapped at `/static/*` drops that prefix and serves `/static/app.css` from `<webapp>/app.css`, so every
  request 404s against a `webapp/static` directory that looks perfectly correct. `false` keeps the whole
  path within the context and reads `<webapp>/static/app.css`, as Jetty 11 did.

  The alternative, if you'd rather keep the new default, is to point the servlet at the subdirectory instead
  with `baseResource=static` — a relative value is resolved against the context's base resource. Either
  works; don't do both.

```java
final ServletHolder staticFiles = new ServletHolder(ResourceServlet.class);
staticFiles.setAsyncSupported(true);
staticFiles.setInitParameter("dirAllowed", "false");
staticFiles.setInitParameter("pathInfoOnly", "false");
staticFiles.setInitParameter("cacheControl", "public,max-age=86400");
staticFiles.setInitParameter("etags", "true");

final Server server = new CuracaoJettyServer.Builder("/myapp")
        .addServlet(staticFiles, "/static/*")
        .addServlet(new ServletHolder(new HealthServlet()), "/health")
        .build();
```

Without a static servlet, `/static/...` is just another path for Curacao, and it answers 404 unless a
controller maps it.

## Base resources

Jetty 12 removed the `Resource.newResource(...)` statics. A `Resource` now comes from a `ResourceFactory`,
which owns the lifecycle of what it hands out:

```java
// Resources that live as long as the JVM: right for a base resource.
ResourceFactory.root().newResource(webappDirectory);

// Resources released when a component stops.
ResourceFactory.of(server).newResource(someDirectory);
```

You only need this when calling `setBaseResource` yourself; the default lookup does it internally, with
`ResourceFactory.root()`.

## Reading the base resource

Components that load templates or files from the webapp directory get it from the servlet context:

```java
@Component
public final class Templates {

    private final Resource baseResource_;

    @Injectable
    public Templates(
            final ServletContext context) {
        baseResource_ = (Resource) context.getAttribute(
                CuracaoJettyServer.BASE_RESOURCE_CONTEXT_ATTRIBUTE);
    }

}
```

Note the attribute's value is keyed per module: this one is `curacao.jetty12.base-resource`, where
`curacao-jetty11-server` used `curacao.jetty11.base-resource`. Read it through the constant and it doesn't
matter.

## Error pages

`setErrorPage("/forward-errors")` forwards every error the container sends to that path, which the Curacao
dispatcher handles like any other request. That covers a static file servlet's 404s, a refused directory
listing, and anything else that calls `sendError()`. Map the path to a controller to render your own error
page:

```java
@Controller
public final class Errors {

    @Route("^/forward-errors$")
    public String error() {
        return "Something went wrong.";
    }

}
```

Or leave the path unmapped: Curacao then raises its `PathNotFoundException`, and your exception mapper
renders the page the same way it renders any other 404.

Requests that reach Curacao but match no route don't go through the error page; your exception mappers
handle those directly.

## Filters, handlers and anything else

Anything the builder doesn't cover goes through the two hooks. Each hook runs after the builder is done, in
the order you added it, so it sees the finished context or server.

```java
final Server server = new CuracaoJettyServer.Builder("/myapp")
        // A filter on every request, including forwards.
        .customizeContext(context -> context.addFilter(TimingFilter.class, "/*",
                EnumSet.of(DispatcherType.FORWARD, DispatcherType.REQUEST)))
        // Gzip JSON and plain text responses.
        .customizeServer(server -> {
            final GzipHandler gzip = new GzipHandler();
            gzip.addIncludedMimeTypes("application/json", "text/plain");
            gzip.setHandler(server.getHandler());
            server.setHandler(gzip);
        })
        .build();
```

When `customizeServer` runs, the server's handler is already the context, so wrapping it, as above, wraps
the whole application.

## Connectors

By default the server has one HTTP/1.1 `ServerConnector` on the builder's host, port and idle timeout,
which sends no `Server` or `X-Powered-By` header.

To adjust that connector, use `customizeConnector`. Its HTTP configuration is reachable through the
connector, for example to trust `X-Forwarded-*` headers from a reverse proxy:

```java
final Server server = new CuracaoJettyServer.Builder("/myapp")
        .customizeConnector(connector -> {
            connector.setAcceptQueueSize(128);
            connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration()
                    .addCustomizer(new ForwardedRequestCustomizer());
        })
        .build();
```

A hand-written `HttpConfiguration.Customizer` has a different shape in Jetty 12: it's given the request and
the response's headers, and returns the request (or a wrapper of it), rather than reaching through the
request for the response:

```java
config.addCustomizer((request, responseHeaders) -> {
    responseHeaders.put("X-Served-By", hostname);
    return request;
});
```

To use a different connector, such as HTTPS, pass a factory. It gets the server and returns the connector.
The builder's `setHost`, `setPort` and `setIdleTimeout` apply only to the default connector, so the factory
sets its own. Start from `CuracaoJettyServer.newHttpConfiguration()` to keep the server headers hidden:

```java
final Server server = new CuracaoJettyServer.Builder("/myapp")
        .setConnectorFactory(s -> {
            final HttpConfiguration config = CuracaoJettyServer.newHttpConfiguration();
            config.addCustomizer(new SecureRequestCustomizer());

            final SslContextFactory.Server ssl = new SslContextFactory.Server();
            ssl.setKeyStorePath("/path/to/keystore.p12");
            ssl.setKeyStorePassword(keyStorePassword);

            final ServerConnector connector = new ServerConnector(s, ssl, new HttpConnectionFactory(config));
            connector.setHost("0.0.0.0");
            connector.setPort(8443);
            return connector;
        })
        .build();
```

`build()` refuses a connector customizer combined with a factory, since the factory replaces the connector
the customizer would change. To serve on several connectors, return one from the factory or keep the default,
and add the others with `customizeServer(server -> server.addConnector(...))`.

## When the application fails to boot

If Curacao can't start your application, for example because a controller asks for an interface that two
components implement, the error is logged and handling depends on `setFailOnStartupError`:

- **`false` (the default):** `server.start()` succeeds, and Jetty marks the context unavailable, so every
  request under the context path gets a 503. An HTTP health check against a path under the context sees
  the application as down. A TCP or process check doesn't, since the port is open and the process is
  running.
- **`true`:** `server.start()` throws the error. If you want the process to exit, stop the server and call
  `System.exit(1)` yourself: other non-daemon threads (Jetty's, or ones your components started) can keep
  the JVM running after `main()` returns.

```java
final Server server = new CuracaoJettyServer.Builder("/myapp")
        .setFailOnStartupError(true)
        .build();
try {
    server.start();
} catch (final Exception e) {
    LOG.error("Startup failed.", e);
    server.stop();
    System.exit(1);
}
server.join();
```

Either way, Curacao destroys the components it had already initialized, so a failed boot doesn't leave
their threads or scheduled jobs running.

## Testing

Build the server with port `0`, start it, and read the port Jetty picked:

```java
final Server server = new CuracaoJettyServer.Builder("/myapp")
        .setPort(0)
        .setBaseResource(ResourceFactory.root().newResource(webappDirectory))
        .setStopAtShutdown(false)
        .build();
server.start();
final int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
// ... send requests to http://127.0.0.1:<port>/myapp/...
server.stop();
```

Each server builds its own controllers and components, so several can boot in one JVM, one after another or
side by side. Curacao does read its configuration, and scan the boot package, once per JVM, so test classes
that boot different applications (a different `curacao.boot-package`) each need their own JVM:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <reuseForks>false</reuseForks>
    </configuration>
</plugin>
```

## Moving from Jetty 11

The builder API is unchanged, so most of the move is imports. In order of how likely each is to bite:

| | `curacao-jetty11-server` | `curacao-jetty12-server` |
|---|---|---|
| Static file servlet | `DefaultServlet` at `/static/*` | `ResourceServlet` at `/static/*`, **plus `pathInfoOnly=false`** (see [Static files](#static-files-and-other-servlets)) — otherwise every static request 404s |
| Dependency | `jetty-server`, `jetty-servlet`, `jetty-webapp` | `jetty-server`, `jetty-ee11-servlet`, `jetty-ee11-webapp` |
| `ServletHolder`, `FilterHolder`, `ErrorPageErrorHandler` | `org.eclipse.jetty.servlet.*` | `org.eclipse.jetty.ee11.servlet.*` |
| `WebAppContext` | `org.eclipse.jetty.webapp` | `org.eclipse.jetty.ee11.webapp` |
| Building a `Resource` | `Resource.newResource(path)` | `ResourceFactory.root().newResource(path)` |
| `HttpConfiguration.Customizer` | `(connector, config, request) -> ...` | `(request, responseHeaders) -> ... ; return request;` |
| Base resource attribute | `curacao.jetty11.base-resource` | `curacao.jetty12.base-resource` |
| `Server`, `ServerConnector`, `HttpConfiguration`, `HttpConnectionFactory`, `QueuedThreadPool` | `org.eclipse.jetty.server.*` / `util.thread.*` | unchanged |

A subclass of `DefaultServlet` needs more than a renamed import, because the hooks moved: Jetty 11 let a
subclass override `getResource(String pathInContext)`, while Jetty 12 resolves content through a
`ResourceService` and a `getEncodedPathInContext(...)` override on `ResourceServlet`.

## Example

`curacao-embedded`, in this repository, is a complete application built with `curacao-jetty11-server`,
including a static file servlet and the filter and gzip hooks above; the differences listed above are what
it would take to point it at this module instead.
