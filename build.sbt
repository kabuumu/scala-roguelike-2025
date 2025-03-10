ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.1"

lazy val root = (project in file("."))
  .settings(
    name := "scala-roguelike"
  )

libraryDependencies ++= Seq(
  "org.typelevel" %% "shapeless3-deriving" % "3.4.0",
  "org.scalafx" %% "scalafx" % "23.0.1-R34"
)

