enablePlugins(ScalaJSPlugin)

version := "0.1.0-SNAPSHOT"
scalaVersion := "3.6.4"
name := "scala-roguelike"

scalaJSUseMainModuleInitializer := true

lazy val root = (project in file("."))
  .settings(
    name := "scala-roguelike",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test,
      "io.indigoengine" %%% "indigo" % "0.21.1"
    )
  )
