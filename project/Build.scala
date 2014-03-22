/**
 * Copyright (c) 2014 Mark S. Kolich
 * http://mark.koli.ch
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

import sbt._
import sbt.Keys._

import com.earldouglas.xsbtwebplugin._
import PluginKeys._
import WebPlugin._

import sbtassembly.Plugin._
import AssemblyKeys._

object Dependencies {

  private val servlet30 = "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided" // Provided by container
  //private val servlet31 = "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided" // Provided by container

  // Jetty 9 stable, version 9.1.1.v20140108 (as of 2/7/14)
  private val jettyWebApp = "org.eclipse.jetty" % "jetty-webapp" % "9.1.1.v20140108"
  private val jettyPlus = "org.eclipse.jetty" % "jetty-plus" % "9.1.1.v20140108"
  private val jettyJsp = "org.eclipse.jetty" % "jetty-jsp" % "9.1.1.v20140108"

  private val jspApi = "javax.servlet.jsp" % "jsp-api" % "2.2" % "provided" // Provided by container
  private val javaxEl = "javax.el" % "javax.el-api" % "3.0.0" % "provided" // Provided by container

  private val reflections = "org.reflections" % "reflections" % "0.9.9-RC1" % "compile" exclude("dom4j", "dom4j")
  
  private val typesafeConfig = "com.typesafe" % "config" % "1.0.2" % "compile"
  
  private val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1" % "compile"
  private val commonsIo = "commons-io" % "commons-io" % "2.4" % "compile"
  private val commonsCodec = "commons-codec" % "commons-codec" % "1.6" % "compile"
  
  private val guava = "com.google.guava" % "guava" % "16.0.1" % "compile"
  private val findBugs = "com.google.code.findbugs" % "jsr305" % "2.0.3" % "compile"
  
  private val slf4j = "org.slf4j" % "slf4j-api" % "1.7.2" % "compile"
  private val logback = "ch.qos.logback" % "logback-core" % "1.0.7" % "compile"
  private val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.0.7" % "compile" // An Slf4j impl
  
  private val gson = "com.google.code.gson" % "gson" % "2.2.4" % "compile"

  private val jacksonCore = "com.fasterxml.jackson.core" % "jackson-core" % "2.2.3" % "compile"
  private val jacksonAnnotations = "com.fasterxml.jackson.core" % "jackson-annotations" % "2.2.3" % "compile"
  private val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.3" % "compile"

  private val asyncHttpClient = "com.ning" % "async-http-client" % "1.7.21" % "compile"
  private val kolichCommon = "com.kolich" % "kolich-common" % "0.2" % "compile"

  val curacaoDeps = Seq(
    servlet30,
    reflections,
    slf4j,
    commonsLang3, commonsIo, commonsCodec,
    guava, findBugs,
	typesafeConfig
  )
  
  val curacaoExampleDeps = Seq(
    servlet30,
    jettyWebApp % "container",
    jettyPlus % "container",
    jettyJsp % "container",
    jspApi, javaxEl,
    logback, logbackClassic,
    asyncHttpClient,
    kolichCommon
  )
  		
  val curacaoGsonDeps = Seq(
    gson
  )

  val curacaoJacksonDeps = Seq(
    jacksonCore, jacksonAnnotations, jacksonDatabind
  )

  val curacaoEmbeddedDeps = Seq(
    servlet30,
    jettyWebApp % "compile",
    logback, logbackClassic
  )

}

object Resolvers {

  private val kolichRepo = "Kolich repo" at "http://markkolich.github.io/repo"

  val depResolvers = Seq(kolichRepo)

}

object Curacao extends Build {

  import Dependencies._
  import Resolvers._

  private val curacaoName = "curacao"
  private val curacaoExamplesName = "curacao-examples"
  private val curacaoGsonName = "curacao-gson"
  private val curacaoJacksonName = "curacao-jackson"
  private val curacaoEmbeddedName = "curacao-embedded"
  
  private val curacaoVersion = "2.0-RC1"
  private val curacaoOrg = "com.kolich.curacao"
    
  private object CuracaoProject extends Plugin {
    
    def apply(moduleName: String,
      moduleVersion: String,
      moduleOrg: String,
      base: File = file("."),
      publishReady: Boolean = false,
      dependencies: Seq[ModuleID] = Nil,
      settings: => Seq[Setting[_]] = Seq()): Project = {
      
      lazy val curacaoSettings = Defaults.defaultSettings ++
        Seq(
          version := moduleVersion,
          organization := moduleOrg,
          scalaVersion := "2.10.2",
          resolvers := depResolvers,
          scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xcheckinit", "-encoding", "utf8"),
          javacOptions ++= Seq("-Xlint", "-encoding", "utf8", "-g"),
          shellPrompt := { (state: State) => { "%s:%s> ".format(moduleName, moduleVersion) } },
          // True to export the packaged JAR instead of just the compiled .class files.
          exportJars := true,
          // Disable using the Scala version in output paths and artifacts.
          // When running 'publish' or 'publish-local' SBT would append a
          // _<scala-version> postfix on artifacts. This turns that postfix off.
          crossPaths := false,
          // Keep the scala-lang library out of the generated POM's for this artifact.
          autoScalaLibrary := false,
          unmanagedSourceDirectories in Compile <<= baseDirectory(new File(_, "src/main/java"))(Seq(_)),
          unmanagedSourceDirectories in Test <<= baseDirectory(new File(_, "src/test/java"))(Seq(_)),
          unmanagedSourceDirectories in Compile in packageSrc <<= baseDirectory(new File(_, "src/main/java"))(Seq(_)),
          classDirectory in Compile <<= baseDirectory(new File(_, "target/classes")),
          retrieveManaged := true,
          libraryDependencies ++= dependencies,
          // Override the default 'package' path used by SBT. Places the resulting
          // JAR into a more meaningful location.
          artifactPath in (Compile, packageBin) ~= { defaultPath =>
            file("dist") / defaultPath.getName
          },
          // Override the default 'test:package' path used by SBT. Places the
          // resulting JAR into a more meaningful location.
          artifactPath in (Test, packageBin) ~= { defaultPath =>
            file("dist") / "test" / defaultPath.getName
          }
        ) ++ (
          // Is this project publish ready?
          publishReady match {
            case true => Seq(
              // Tweaks the name of the resulting JAR on a "publish" or "publish-local".
              artifact in packageBin in Compile <<= (artifact in packageBin in Compile, version) apply ((artifact, ver) => {
                val newName = artifact.name + "-" + ver
                Artifact(newName, artifact.`type`, artifact.extension, artifact.classifier, artifact.configurations, artifact.url)
              }),
              // Tweaks the name of the resulting source JAR on a "publish" or "publish-local".
              artifact in packageSrc in Compile <<= (artifact in packageSrc in Compile, version) apply ((artifact, ver) => {
                val newName = artifact.name + "-" + ver
                Artifact(newName, artifact.`type`, artifact.extension, artifact.classifier, artifact.configurations, artifact.url)
              }),
              // Tweaks the name of the resulting POM on a "publish" or "publish-local".
              artifact in makePom <<= (artifact in makePom, version) apply ((artifact, ver) => {
                val newName = artifact.name + "-" + ver
                Artifact(newName, artifact.`type`, artifact.extension, artifact.classifier, artifact.configurations, artifact.url)
              }),
              // Override the global name of the artifact.
              artifactName <<= (name in (Compile, packageBin)) { projectName =>
                (config: ScalaVersion, module: ModuleID, artifact: Artifact) =>
                  var newName = projectName
                  if(module.revision.nonEmpty) {
                    newName += "-" + module.revision
                  }
                  newName + "." + artifact.extension
              },
              // Do not bother trying to publish artifact docs (scaladoc, javadoc). Meh.
              publishArtifact in packageDoc := false
            )
            case _ => Seq()
          }
        )
      
      lazy val allSettings = curacaoSettings ++ settings
      Project(moduleName, base, settings = allSettings)
      
    }
    
  }
  
  lazy val curacao: Project = CuracaoProject(
    moduleName = curacaoName,
    moduleVersion = curacaoVersion,
    moduleOrg = curacaoOrg,
    base = file("curacao"),
    publishReady = true,
    dependencies = curacaoDeps
  )
    
  lazy val curacaoGson: Project = CuracaoProject(
    moduleName = curacaoGsonName,
    moduleVersion = curacaoVersion,
    moduleOrg = curacaoOrg,
    base = file("curacao-gson"),
    publishReady = true,
    dependencies = curacaoGsonDeps
  ) dependsOn(curacao)

  lazy val curacaoJackson: Project = CuracaoProject(
    moduleName = curacaoJacksonName,
    moduleVersion = curacaoVersion,
    moduleOrg = curacaoOrg,
    base = file("curacao-jackson"),
    publishReady = true,
    dependencies = curacaoJacksonDeps
  ) dependsOn(curacao)

  lazy val curacaoEmbedded: Project = CuracaoProject(
    moduleName = curacaoEmbeddedName,
    moduleVersion = curacaoVersion,
    moduleOrg = curacaoOrg,
    base = file("curacao-embedded"),
    dependencies = curacaoEmbeddedDeps,
    settings = sbtassembly.Plugin.assemblySettings ++ Seq(
      mainClass in assembly := Some("com.kolich.curacao.embedded.ServerBootstrap"),
      outputPath in assembly := file("dist") / "curacao-embedded.jar",
      assemblyOption in assembly ~= { _.copy(includeScala = false) },
      sbt.Keys.test in assembly := {}
    )
  ) dependsOn(curacao, curacaoGson)

  lazy val curacaoExamples: Project = CuracaoProject(
    moduleName = curacaoExamplesName,
    moduleVersion = curacaoVersion,
    moduleOrg = curacaoOrg,
    base = file("curacao-examples"),
    dependencies = curacaoExampleDeps,
    settings = webSettings ++ Seq(
        // Overrides the default context path used for this project.  By
        // default, the context path is "/", but here we're overriding it
        // so that the application is available under "/curacao" instead.
        apps in container.Configuration <<= (deployment in DefaultConf) map {
          d => Seq("/curacao" -> d)
        },
        warPostProcess in Compile <<= target map {
          // Ensures the src/main/webapp/WEB-INF/work directory is NOT included
          // in the packaged WAR file.  This is a temporary directory used by
          // the application and Servlet container in development that should
          // not be shipped with a build.
          (target) => { () => {
            val webinf = target / "webapp" / "WEB-INF"
            IO.delete(webinf / "work") // recursive
          }}
        },
        // Change the location of the packaged WAR file as generated by the
        // xsbt-web-plugin to place it into the dist/ directory.
        artifactPath in (Compile, packageWar) ~= { defaultPath =>
          file("dist") / defaultPath.getName
        },
	      unmanagedSourceDirectories in Compile <++= baseDirectory(new File(_, "src/main/scala"))(Seq(_)),
	      unmanagedSourceDirectories in Test <++= baseDirectory(new File(_, "src/test/scala"))(Seq(_)),
	      // Add the local 'config' directory to the classpath at runtime,
	      // so anything there will ~not~ be packaged with the application deliverables.
	      // Things like application configuration .properties files go here in
	      // development and so these will not be packaged+shipped with a build.
	      // But, they are still available on the classpath during development,
	      // like when you run Jetty via the xsbt-web-plugin that looks for some
	      // configuration file or .properties file on the classpath.
	      unmanagedClasspath in Runtime <+= baseDirectory map { bd => Attributed.blank(bd / "config") },
	      autoScalaLibrary := true
	    )
    ) dependsOn(curacao, curacaoGson, curacaoJackson)
    
}
