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
- **curacao-embedded** - Embedded server support
- **curacao-examples** - Working examples demonstrating framework usage

## Architecture

### Core Annotations
- `@Controller` - Marks a class as a request handler
- `@Component` - Marks a class for dependency injection (supports constructor injection via `@Injectable`)
- `@Mapper` - Marks request argument or response type mappers
- `@RequestMapping` - Maps HTTP methods and URL patterns to controller methods (regex-based by default)

### Request Flow
1. Requests hit `CuracaoDispatcherServlet` (async servlet)
2. `RequestMappingTable` matches URL patterns to controller methods
3. `AbstractControllerArgumentMapper` implementations resolve method parameters from the request
4. Controller method executes
5. `AbstractControllerReturnTypeMapper` implementations render the response

### Component System
Components are discovered via classpath scanning within the configured `boot-package`. The `ComponentTable` handles:
- Automatic dependency injection via `@Injectable` constructor annotation
- Lifecycle management (`ComponentInitializable`, `ComponentDestroyable` interfaces)
- Component suppression via `@Component(value = {"pattern.to.suppress.*"})`

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
