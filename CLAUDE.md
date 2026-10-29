# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Curacao is a REST/HTTP toolkit for building integration layers on top of asynchronous servlets. It provides annotation-based request routing, dependency injection, and flexible request/response mapping.

## Build Commands

```bash
# Compile and package all modules
mvn clean package

# Run the examples application locally (Jetty on port 8080)
mvn -am -pl curacao-examples package -Pjetty-run
# Access at http://localhost:8080/curacao

# Check for dependency updates
mvn versions:display-dependency-updates
```

## Module Structure

- **curacao** - Core library with annotations, mappers, and component system
- **curacao-servlet** - Abstract servlet support layer
- **curacao-servlet-jakarta** - Jakarta Servlet 5 (Jetty 11+) implementation
- **curacao-servlet-javax** - Javax Servlet 3.1 (legacy) implementation
- **curacao-gson** - Gson JSON serialization support
- **curacao-jackson** - Jackson JSON serialization support
- **curacao-freemarker** - FreeMarker template rendering: `FreeMarkerContent`, `Utf8TextEntity` (a text body sent with its Guava `MediaType` as given, encoded in that type's charset or UTF-8; pairing the type with suitable transformers is up to the caller), `CuracaoFreeMarkerException`, and abstract bases for the renderer (`AbstractFreeMarkerContentRenderer`) and response mappers (`AbstractFreeMarkerContentResponseMapper<T>`, whose `renderFreeMarkerContent()` serves both a `FreeMarkerContent` mapper and exception mappers that render a template; if the template fails to render, it logs the error and sends a plain-text 500, since an exception thrown from a mapper would otherwise leave an empty 200). They're abstract because Curacao only scans the app's boot package, so apps register them with their own `@Component`/`@Mapper` subclasses. Apps build and pass in their own `freemarker.template.Configuration`; Curacao doesn't configure FreeMarker. Content types are Guava `MediaType`s. Rendered bodies run through the `TextTransformer`s each `FreeMarkerContent` adds with `withTransformers(...)`, in order (none by default, so the body is written as-is). A transformer that throws or returns null is logged at WARN and skipped, passing its input through unchanged. The built-in transformers (in `curacao.freemarker.transformers.html`) implement `TextTransformer` and are passed as instances, e.g. `withTransformers(new HtmlMinifier(), new HtmlCompressor())`. Both are stateless, so one instance can be shared across requests (`HtmlCompressor` uses a new htmlcompressor + YUI compressor per call). htmlcompressor and yuicompressor are optional dependencies, used only by `HtmlCompressor`
- **curacao-mcp** - Model Context Protocol server (JSON-RPC over stateless Streamable HTTP, plus a stdio bridge); see its README
- **curacao-jetty11-server** - `CuracaoJettyServer.Builder` builds (doesn't start) an embedded Jetty 11 `Server` hosting a Curacao app: a `WebAppContext` (no sessions, security or JSP) with the Curacao listener and dispatcher at `/*` and any servlets the app adds with `addServlet(holder, pathSpec)` (no static servlet unless added; apps configure their own `DefaultServlet` holder, with `dirAllowed=false` since Jetty defaults it to true), `Server`/`X-Powered-By` headers off, the base resource (`src/main/webapp`, else `webapp` on the classpath) stored under `BASE_RESOURCE_CONTEXT_ATTRIBUTE`. Options for thread names (`setThreadNameFormat`, a Guava `ThreadFactoryBuilder` name format, default `jetty-%d`), idle timeout, `customizeConnector` (tweak the default connector) or `setConnectorFactory` (replace it; `newHttpConfiguration()` keeps the server headers hidden; the two can't be combined), error page, `setFailOnStartupError` (default: a failed boot leaves the context answering 503), and `customizeContext`/`customizeServer` hooks. Tests boot apps on port 0; several servers can share a JVM, but test classes that boot a different `boot-package` need their own (configuration and the boot-package scan are read once per JVM)
- **curacao-embedded** - Example embedded app (fat JAR) built with `curacao-jetty11-server`
- **curacao-examples** - Working examples demonstrating framework usage

## Architecture

### Core Annotations
- `@Controller` - Marks a class as a request handler
- `@Component` - Marks a class for dependency injection (supports constructor injection via `@Injectable`)
- `@Mapper` - Marks request argument or response type mappers
- `@Route` - Maps HTTP methods and URL patterns to controller methods (regex-based by default)

### Request Flow
1. Requests hit `CuracaoDispatcherServlet` (async servlet). The request context captures the path within the application on the container's thread, during the dispatch: once `service()` returns, a forwarded or error-page request reverts to its original URI, and the rest of the request runs on Curacao's thread pool. The path is the request URI without the context path; the servlet API reports the root context's path as `""`, in which case the whole URI is the path (`UrlPathHelper`)
2. `RouteTable` matches URL patterns to controller methods. Every route whose pattern matches is collected, and the most specific handles the request, as in Spring (`RouteSpecificity`, computed once per route): fewer variables plus wildcards wins, then more literal characters, then fewer wildcards. For regex routes, capturing groups are variables; character classes, `.`, class escapes like `\d`, quantifiers and top-level `|` are wildcards; other characters outside groups are literals. Ant routes count `{name}` as a variable and `*`/`?`/`**` as wildcards; a custom matcher's routes rank last. Two matching routes that are equally specific fail the request (a `CuracaoException`, so a 500) naming both. At startup, a mapping (HTTP method, matcher and pattern) mapped to two handlers, or a regex route that doesn't compile, fails with `ComponentConfigurationException`; a method overriding an annotated superclass method counts as one handler
3. `AbstractControllerArgumentMapper` implementations resolve method parameters from the request. For each parameter type, the app's mappers are asked in mapper class name order, then Curacao's built-in ones; the first to return non-null wins (several mappers per type are allowed, e.g. one per annotation). `@RequestBody` buffers the body up to `max-request-body-size` (or the annotation's `maxSizeInBytes`): a larger body is refused with `RequestTooLargeException` (413), with or without a `Content-Length` (a chunked body is read one byte past the limit to tell), and a charset in `Content-Type` that's malformed or unsupported gets `UnsupportedMediaTypeException` (415)
4. Controller method executes
5. `AbstractControllerReturnTypeMapper` implementations render the response (`MapperTable.getReturnTypeMapperForClass`). The app's mappers come first: of those whose type the result is assignable to, the most specific type wins (so a `ResourceNotFoundException` mapper beats an `Exception` mapper, and an app `Exception` mapper still renders Curacao's routing exceptions, which have a more specific built-in mapper); only then the most specific built-in mapper, with `Object` the catch-all. Ties go to the first in mapper class name order. Two app mappers for the same type fail startup

### Component System
Components are discovered via classpath scanning within the configured `boot-package` and its subpackages, and nowhere else (Reflections is given an input filter, since `ClasspathHelper.forPackage` alone yields whole classpath roots, e.g. every library in a fat JAR). The `ComponentTable` creates each one once (in name order, dependencies first) and resolves every `@Injectable` constructor parameter, for components, controllers, filters and mappers alike (`ComponentTable.resolveArguments`). Resolution never guesses:
- A component class asked for by its own type gets that component. Otherwise a parameter gets the one component assignable to its type (class, superclass or interface, except the lifecycle interfaces); with several, the one marked `@Component(primary = true)`; several and no single primary, or no match at all, fails startup with `ComponentConfigurationException`/`ArgumentRequiredException`. Nothing is ever injected as null.
- `List<T>` (or `List<? extends T>`) gets every component assignable to `T`, sorted by class name; it may be empty. A raw `List` fails.
- `@Component(replaces = X.class)` removes `X`: it's never created, and asking for it by its own class fails, naming the replacement. `X` must be a component; self-replacement and replacement cycles fail. `@Component(suppress = "com.example.*")` does the same by canonical-name wildcard, for classes that may not be on the classpath; a pattern matching nothing logs a warning.
- At most one constructor per class may be `@Injectable` (as in JSR-330); more than one throws `MultipleInjectableConstructorsException`. It's found by reflection on the class itself (`CuracaoReflectionUtils.getInjectableConstructorForClass`), not the classpath index.
- A class without an `@Injectable` constructor needs a public no-argument constructor (`CuracaoReflectionUtils.getNoArgConstructorForClass`); otherwise startup fails with `ComponentConfigurationException`, rather than calling a constructor with nulls. This applies to components, controllers, path matchers, filters and mappers.
- Controllers, path matchers and filters are created once per context: `RouteTable` owns their instances (shared by its routes), so a restarted context, or a second one in the same JVM, builds its own, wired to its own components.
- Configuration errors extend `AbstractConfigurationException`. `RouteTable` and `MapperTable` look for one anywhere in a failure's cause chain (`rethrowIfPresent`) and fail startup with it; they log and skip a route or mapper only for other failures.
- Lifecycle management (`ComponentInitializable`, `ComponentDestroyable` interfaces): each component is initialized once in creation order and destroyed once in reverse order.
- `ComponentTableTest` and `MapperTableTest` build tables from explicit classes through package-private constructors, without scanning. Behavior that needs scanning (routing, startup failures) is tested end to end in curacao-jetty11-server, one fixture package per application.

### Configuration
Uses Typesafe Config (HOCON format). Application config in `application.conf` overrides defaults in `reference.conf`:

```hocon
curacao {
  boot-package = "your.app.package"
  async-context-timeout = 30s
  thread-pool.size = 256
  mappers.request.max-request-body-size = 16k
}
```

## Code Style Requirements

The project enforces strict code style via Checkstyle and PMD (run during `prepare-package` phase):

- **Line length**: 120 characters max
- **Indentation**: 4 spaces (no tabs)
- **Class member naming**: Must end with underscore (e.g., `componentTable_`)
- **Local variables**: Must be at least 3 characters (e.g., `ctx` not `ct`)
- **All non-exception classes must be declared `final`**
- **All method/constructor parameters must be `final`**
- **All local variables must be `final`**
- **Method arguments must start on a new line** (not on the same line as method signature)
- **License header required** on all Java files (MIT license from `build/checkstyle/LICENSE.txt`)

## Servlet API Versions

Choose the appropriate servlet module based on your container:
- **Jakarta Servlet 5** (Jetty 11+, Tomcat 10+): Use `curacao-servlet-jakarta`
- **Javax Servlet 3.1** (Jetty 9, Tomcat 9): Use `curacao-servlet-javax`
