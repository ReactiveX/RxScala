import bintray.Keys._

organization := "io.reactivex"

name := "rxscala"

version := "1.0.0-RC1-SNAPSHOT"

lazy val root = project in file(".")

lazy val examples = project in file("examples") dependsOn root

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

libraryDependencies ++= Seq(
  "io.reactivex" % "rxjava" % "1.0.0-rc.3",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "junit" % "junit-dep" % "4.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.2" % "test")

bintrayPublishSettings

repository in bintray := "RxJava"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization in bintray := Some("reactivex")

packageLabels := Seq("RxScala")
