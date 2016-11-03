organization := "io.reactivex"

name := "rxscala"

lazy val root = project in file(".")

lazy val examples = project in file("examples") dependsOn (root % "test->test;compile->compile") settings(
  libraryDependencies ++= Seq(
    "org.apache.bcel" % "bcel" % "5.2" % "test"
  )
)

scalacOptions in ThisBuild := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-Xfatal-warnings")

scalaVersion in ThisBuild := "2.12.0"

crossScalaVersions in ThisBuild := Seq("2.10.6", "2.11.8", "2.12.0")

parallelExecution in Test := false

libraryDependencies ++= {
  // Scalatest 3.0.0 supports 2.12.0-RC1 but it drops Scala 2.10 support. So just use 2.2.6 for Scala 2.10.
  val scalatestVersion = if (scalaVersion.value.startsWith("2.10")) "2.2.6" else "3.0.0"
  Seq(
    "io.reactivex" % "rxjava" % "1.2.0",
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test")
}

// Set up the doc mappings
// See http://stackoverflow.com/questions/16934488/how-to-link-classes-from-jdk-into-scaladoc-generated-doc
autoAPIMappings := true

val externalJavadocMap = Map(
  "rxjava" -> "http://reactivex.io/RxJava/javadoc/index.html"
)

/*
 * The rt.jar file is located in the path stored in the sun.boot.class.path system property.
 * See the Oracle documentation at http://docs.oracle.com/javase/6/docs/technotes/tools/findingclasses.html.
 */
val rtJar: String = System.getProperty("sun.boot.class.path").split(java.io.File.pathSeparator).collectFirst {
  case str: String if str.endsWith(java.io.File.separator + "rt.jar") => str
}.get // fail hard if not found

val javaApiUrl: String = "http://docs.oracle.com/javase/8/docs/api/index.html"

val allExternalJavadocLinks: Seq[String] = javaApiUrl +: externalJavadocMap.values.toSeq

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

def javadocLinkRegex(javadocURL: String): Regex = ("""\"(\Q""" + javadocURL + """\E)#([^"]*)\"""").r

def hasJavadocLink(f: File): Boolean = allExternalJavadocLinks exists {
  javadocURL: String =>
    (javadocLinkRegex(javadocURL) findFirstIn IO.read(f)).nonEmpty
}

val fixJavaLinks: Match => String = m =>
  m.group(1) + "?" + m.group(2).replace(".", "/") + ".html"

apiMappings ++= {
  // Lookup the path to jar from the classpath
  val classpath = (fullClasspath in Compile).value
  def findJar(nameBeginsWith: String): File = {
    classpath.find { attributed: Attributed[File] => (attributed.data ** s"$nameBeginsWith*.jar").get.nonEmpty }.get.data // fail hard if not found
  }
  // Define external documentation paths
  (externalJavadocMap map {
    case (name, javadocURL) => findJar(name) -> url(javadocURL)
  }) + (file(rtJar) -> url(javaApiUrl))
}

// Override the task to fix the links to JavaDoc
doc in Compile <<= (doc in Compile) map { target: File =>
  (target ** "*.html").get.filter(hasJavadocLink).foreach { f =>
    val newContent: String = allExternalJavadocLinks.foldLeft(IO.read(f)) {
      case (oldContent: String, javadocURL: String) =>
        javadocLinkRegex(javadocURL).replaceAllIn(oldContent, fixJavaLinks)
    }
    IO.write(f, newContent)
  }
  target
}
