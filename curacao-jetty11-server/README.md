# curacao-jetty11-server

Builds an embedded [Jetty 11](https://jetty.org) server that hosts a Curacao application, so an application's
`main()` doesn't have to assemble the thread pool, connector, servlet context and Curacao dispatcher by
hand. Like the rest of Curacao, it requires Java 11 or later.

`CuracaoJettyServer.Builder` builds a Jetty `Server` but doesn't start it. Your `main()` starts it, and tests
can boot the whole application on a free port.

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
  `CuracaoJettyServer.BASE_RESOURCE_ATTRIBUTE`, for components that read templates or files at runtime.
- A JVM shutdown hook that stops the server.

## Dependency

```xml
<dependency>
    <groupId>curacao</groupId>
    <artifactId>curacao-jetty11-server</artifactId>
    <version>${curacao.version}</version>
</dependency>
```

It brings in `curacao`, `curacao-servlet-jakarta` and Jetty 11's `jetty-server`, `jetty-servlet` and
`jetty-webapp`.

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
        final ServletHolder holder = new ServletHolder(DefaultServlet.class);
        holder.setAsyncSupported(true);
        holder.setInitParameter("dirAllowed", "false");
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
| `setBaseResource(Resource)` | found for you | The context's "webapp" directory, when the default lookup doesn't fit. |
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
final ServletHolder staticFiles = new ServletHolder(LocalCacheAwareDefaultServlet.class);
staticFiles.setAsyncSupported(true);
staticFiles.setInitParameter("dirAllowed", "false");
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
`ServletHolder` and add with `addServlet(holder, pathSpec)`. Servlets are added in the order given, and
Curacao, at `/*`, answers every path that no servlet claims more specifically.

For static files, add Jetty's `DefaultServlet` (or a subclass). It serves files from the context's base
resource, configured with its init parameters. Set `dirAllowed` to `false`: `DefaultServlet` lists a
directory's contents by default.

```java
final ServletHolder staticFiles = new ServletHolder(DefaultServlet.class);
staticFiles.setAsyncSupported(true);
staticFiles.setInitParameter("dirAllowed", "false");
staticFiles.setInitParameter("cacheControl", "public,max-age=86400");
staticFiles.setInitParameter("etags", "true");

final Server server = new CuracaoJettyServer.Builder("/myapp")
        .addServlet(staticFiles, "/static/*")
        .addServlet(new ServletHolder(new HealthServlet()), "/health")
        .build();
```

Without a static servlet, `/static/...` is just another path for Curacao, and it answers 404 unless a
controller maps it.

## Reading the base resource

Components that load templates or files from the webapp directory get it from the servlet context:

```java
@Component
public final class Templates {

    private final Resource baseResource_;

    @Injectable
    public Templates(
            final ServletContext context) {
        baseResource_ = context.getAttribute(CuracaoJettyServer.BASE_RESOURCE_ATTRIBUTE);
    }

}
```

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
        .setBaseResource(Resource.newResource(webappDirectory))
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

## Example

`curacao-embedded`, in this repository, is a complete application built with this module, including a
static file servlet and the filter and gzip hooks above.
