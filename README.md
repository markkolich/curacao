# Curacao

An open source toolkit for building REST/HTTP-based integration layers on top of asynchronous servlets.

<img src="static/bluedrink.jpg">

## Introduction

Writing comprehensive documentation is hard, but a reasonable introduction to Curacao can be found <a href="https://github.com/markkolich/blog/blob/master/content/entries/introducing-curacao.md">here</a>.

As the project matures, I intend to write out more documentation.

## Latest Version

See the [Releases page](https://github.com/markkolich/curacao/releases) to find the latest version.

## Compiling

Curacao requires Java 11 or later, and is packaged using Maven.

To compile & package the source:

```
mvn clean package
```

Resulting JARs and source JARs will be placed into the local `dist` directory.

## Running the Examples

Working examples that demonstrate Curacao's flexibility can be found in the [curacao-examples project](https://github.com/markkolich/curacao/tree/master/curacao-examples).

In the spirit of "eating my own dog food", [my own blog is built on Curacao and is fully open source on GitHub](https://github.com/markkolich/blog). If you're looking for more complex component definitions, and realistic request mapping and response handling examples, the application source of my blog will be a great start.

To compile and run the examples locally:

```
mvn -am -pl curacao-examples package -Pjetty-run
```

Then hit <a href="http://localhost:8080/curacao">http://localhost:8080/curacao</a> in your favorite browser.

## License

Copyright (c) 2024 <a href="https://mark.koli.ch">Mark S. Kolich</a>

All code in this project is freely available for use and redistribution under the <a href="http://opensource.org/comment/991">MIT License</a>.

See <a href="https://github.com/markkolich/curacao/blob/master/LICENSE">LICENSE</a> for details.
