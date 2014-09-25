organization := "io.reactivex"

name := "rxscala"

version := "1.0.0-RC1-SNAPSHOT"

lazy val root = project in file(".")

lazy val examples = project in file("examples") dependsOn root

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "io.reactivex" % "rxjava" % "1.0.0-rc.3",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "junit" % "junit-dep" % "4.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.2" % "test")
