/**
 * Copyright (c) 2013 Mark S. Kolich
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
import WebappPlugin._

import com.typesafe.sbteclipse.plugin.EclipsePlugin._

object Dependencies {  
  
  private val servlet30 = "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided" // Provided by container
  private val servlet31 = "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided" // Provided by container

  // Jetty 9.1-RC0 (as of 11/1/13)
  private val jetty91WebApp = "org.eclipse.jetty" % "jetty-webapp" % "9.1.0.RC0" % "container"
  private val jetty91Plus = "org.eclipse.jetty" % "jetty-plus" % "9.1.0.RC0" % "container"
  private val jetty91Jsp = "org.eclipse.jetty" % "jetty-jsp" % "9.1.0.RC0" % "container"

  // Jetty 9 "stable", version 9.0.6.v20130930 (as of 10/25/13)
  private val jetty9WebApp = "org.eclipse.jetty" % "jetty-webapp" % "9.0.6.v20130930" % "container"
  private val jetty9Plus = "org.eclipse.jetty" % "jetty-plus" % "9.0.6.v20130930" % "container"
  private val jetty9Jsp = "org.eclipse.jetty" % "jetty-jsp" % "9.0.6.v20130930" % "container"
  
  // Jetty 8 "stable", version 8.1.13.v20130916 (as of 10/25/13)
  private val jetty8WebApp = "org.eclipse.jetty" % "jetty-webapp" % "8.1.13.v20130916" % "container"
  private val jetty8Plus = "org.eclipse.jetty" % "jetty-plus" % "8.1.13.v20130916" % "container"
  private val jetty8Jsp = "org.eclipse.jetty" % "jetty-jsp" % "8.1.13.v20130916" % "container"
  
  def getJettyDependencies:(Seq[sbt.ModuleID],Seq[sbt.ModuleID]) = {
    val version = Option(System.getProperty("jetty.version"))
    version match {
      case Some(v) if "8".equals(v) => (Seq(jetty8WebApp, jetty8Plus, jetty8Jsp), Seq(servlet30)) 
      case Some(v) if "9".equals(v) => (Seq(jetty9WebApp, jetty9Plus, jetty9Jsp), Seq(servlet30))
      case _ => (Seq(jetty91WebApp, jetty91Plus, jetty91Jsp), Seq(servlet31))
    }
  }
  
  private val jspApi = "javax.servlet.jsp" % "jsp-api" % "2.2" % "provided" // Provided by container
  private val javaxEl = "javax.el" % "javax.el-api" % "3.0.0" % "provided" // Provided by container
  
  private val reflections = "org.reflections" % "reflections" % "0.9.9-RC1" % "compile" exclude("dom4j", "dom4j")
  
  private val typesafeConfig = "com.typesafe" % "config" % "1.0.2" % "compile"
  
  private val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1" % "compile"
  private val commonsIo = "commons-io" % "commons-io" % "2.4" % "compile"
  private val commonsCodec = "commons-codec" % "commons-codec" % "1.6" % "compile"
  
  private val guava = "com.google.guava" % "guava" % "15.0" % "compile"
  private val findBugs = "com.google.code.findbugs" % "jsr305" % "2.0.2" % "compile"
  
  private val slf4j = "org.slf4j" % "slf4j-api" % "1.7.2" % "compile"
  private val logback = "ch.qos.logback" % "logback-core" % "1.0.7" % "compile"
  private val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.0.7" % "compile" // An Slf4j impl
  
  private val gson = "com.google.code.gson" % "gson" % "2.2.4" % "compile" 
  private val asyncHttpClient = "com.ning" % "async-http-client" % "1.7.21" % "compile"
    
  val curacaoDeps =
  	// Servlet API dependencies.
  	getJettyDependencies._2 ++
  	// All other dependencies.
  	Seq(reflections,
  		slf4j,
		commonsLang3, commonsIo, commonsCodec,
		guava, findBugs,
		typesafeConfig)
  
  val curacaoExampleDeps =
  	// Jetty container dependencies for the "xsbt-web-plugin". 
  	getJettyDependencies._1 ++ getJettyDependencies._2 ++
  	// All other dependencies.
  	Seq(jspApi, javaxEl,
  		logback, logbackClassic,
  		gson,
  		asyncHttpClient)

}

object Curacao extends Build {

  import Dependencies._
  import Resolvers._

  private val curacaoName = "curacao"
  private val curacaoExamplesName = "curacao-examples"
  
  private val curacaoVersion = "2.0"
  private val curacaoOrg = "com.kolich.curacao"

  lazy val curacao: Project = Project(
    id = curacaoName,
    base = new File("."),
    settings = Defaults.defaultSettings ++ Seq(
      version := curacaoVersion,
      organization := curacaoVersion,
      scalaVersion := "2.10.2",
      javacOptions ++= Seq("-Xlint", "-g"),
      shellPrompt := { (state: State) => { "%s:%s> ".format(curacaoName, curacaoVersion) } },
      // True to export the packaged JAR instead of just the compiled .class files.
      exportJars := true,
      // Disable using the Scala version in output paths and artifacts.
      // When running 'publish' or 'publish-local' SBT would append a
      // _<scala-version> postfix on artifacts. This turns that postfix off.
      crossPaths := false,
      // Keep the scala-lang library out of the generated POM's for this artifact. 
      autoScalaLibrary := false,
      // Only add src/main/java and src/test/java as source folders in the project.
      // Not a "Scala" project at this time.
      unmanagedSourceDirectories in Compile <<= baseDirectory(new File(_, "src/main/java"))(Seq(_)),
      //unmanagedSourceDirectories in Compile <++= baseDirectory(new File(_, "src/examples/java"))(Seq(_)),
      unmanagedSourceDirectories in Test <<= baseDirectory(new File(_, "src/test/java"))(Seq(_)),
      // Tell SBT to include our .java files when packaging up the source JAR.
      unmanagedSourceDirectories in Compile in packageSrc <<= baseDirectory(new File(_, "src/main/java"))(Seq(_)),
      // Override the SBT default "target" directory for compiled classes.
      classDirectory in Compile <<= baseDirectory(new File(_, "target/classes")),
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
      // Do not bother trying to publish artifact docs (scaladoc, javadoc). Meh.
      publishArtifact in packageDoc := false,
      // Override the global name of the artifact.
      artifactName <<= (name in (Compile, packageBin)) { projectName =>
        (config: ScalaVersion, module: ModuleID, artifact: Artifact) =>
          var newName = projectName
          if (module.revision.nonEmpty) {
            newName += "-" + module.revision
          }
          newName + "." + artifact.extension
      },
      // Override the default 'package' path used by SBT. Places the resulting
      // JAR into a more meaningful location.
      artifactPath in (Compile, packageBin) ~= { defaultPath =>
        file("dist") / defaultPath.getName
      },
      // Override the default 'test:package' path used by SBT. Places the
      // resulting JAR into a more meaningful location.
      artifactPath in (Test, packageBin) ~= { defaultPath =>
        file("dist") / "test" / defaultPath.getName
      },
      libraryDependencies ++= curacaoDeps,
      retrieveManaged := true) ++
      Seq(EclipseKeys.createSrc := EclipseCreateSrc.Default,
        // Make sure SBT also fetches/loads the "src" (source) JAR's for
        // all declared dependencies.
        EclipseKeys.withSource := true,
        // This is a Java project, only.
        EclipseKeys.projectFlavor := EclipseProjectFlavor.Java))
        
  lazy val curacaoExamples: Project = Project(
    id = curacaoExamplesName,
    base = new File("examples"),
    // This "examples" project has a dependency on the root "curacao"
    // project above.  That is, if anything changes above, then when this project
    // is run the above will also be compiled automatically.
    dependencies = Seq(curacao),
    settings = Defaults.defaultSettings ++ Seq(
      version := curacaoVersion,
      organization := curacaoOrg,
      scalaVersion := "2.10.2",
      shellPrompt := { (state: State) => { "%s:%s> ".format(curacaoExamplesName, curacaoVersion) } },
      // True to export the packaged JAR instead of just the compiled .class files.
      exportJars := true,
      // Disable using the Scala version in output paths and artifacts.
      // When running 'publish' or 'publish-local' SBT would append a
      // _<scala-version> postfix on artifacts. This turns that postfix off.
      crossPaths := false,
      unmanagedSourceDirectories in Compile <<= baseDirectory(new File(_, "src/main/java"))(Seq(_)),
      unmanagedSourceDirectories in Compile <++= baseDirectory(new File(_, "src/main/scala"))(Seq(_)),
      unmanagedSourceDirectories in Test <<= baseDirectory(new File(_, "src/test/java"))(Seq(_)),
      unmanagedSourceDirectories in Test <++= baseDirectory(new File(_, "src/test/scala"))(Seq(_)),
      // Tell SBT to include our .java files when packaging up the source JAR.
      unmanagedSourceDirectories in Compile in packageSrc <<= baseDirectory(new File(_, "src/main/java"))(Seq(_)),
      // Override the SBT default "target" directory for compiled classes.
      classDirectory in Compile <<= baseDirectory(new File(_, "target/classes")),
      // Add the local 'config' directory to the classpath at runtime,
      // so anything there will ~not~ be packaged with the application deliverables.
      // Things like application configuration .properties files go here in
      // development and so these will not be packaged+shipped with a build.
      // But, they are still available on the classpath during development,
      // like when you run Jetty via the xsbt-web-plugin that looks for some
      // configuration file or .properties file on the classpath.
      unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "config") },
      // Do not bother trying to publish artifact docs (scaladoc, javadoc). Meh.
      publishArtifact in packageDoc := false,
      // Override the global name of the artifact.
      artifactName <<= (name in (Compile, packageBin)) { projectName =>
        (config: ScalaVersion, module: ModuleID, artifact: Artifact) =>
          var newName = projectName
          if (module.revision.nonEmpty) {
            newName += "-" + module.revision
          }
          newName + "." + artifact.extension
      },
      // Override the default 'package' path used by SBT. Places the resulting
      // JAR into a more meaningful location.
      artifactPath in (Compile, packageBin) ~= { defaultPath =>
        file("dist") / "examples" / defaultPath.getName
      },
      // Override the default 'test:package' path used by SBT. Places the
      // resulting JAR into a more meaningful location.
      artifactPath in (Test, packageBin) ~= { defaultPath =>
        file("dist") / "examples" / "test" / defaultPath.getName
      },
      libraryDependencies ++= curacaoExampleDeps,
      retrieveManaged := true) ++
      // xsbt-web-plugin settings
      webSettings ++
      // xsbt-web-plugin overrides
      Seq(
	      warPostProcess in Compile <<= (target) map {
	        // Ensures the src/main/webapp/WEB-INF/work directory is NOT included
	        // in the packaged WAR file.  This is a temporary directory used by
	        // the application and servlet container in development that should
	        // not be shipped with a build.
	        (target) => { () => {
		      val webinf = target / "webapp" / "WEB-INF"
		      IO.delete(webinf / "work") // recursive
	        }}
	      },
	      // Change the location of the packaged WAR file as generated by the
	      // xsbt-web-plugin.
	      artifactPath in (Compile, packageWar) ~= { defaultPath =>
	        file("dist") / defaultPath.getName
	      }
      ) ++
      Seq(EclipseKeys.createSrc := EclipseCreateSrc.Default,
        // Make sure SBT also fetches/loads the "src" (source) JAR's for
        // all declared dependencies.
        EclipseKeys.withSource := true,
        // Important, so that Eclipse doesn't attempt to use relative paths
        // when resolving libraries for this sub-project.
        EclipseKeys.relativizeLibs := false,
        EclipseKeys.projectFlavor := EclipseProjectFlavor.Scala))

}
