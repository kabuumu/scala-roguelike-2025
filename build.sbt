import indigoplugin.*
import indigoplugin.IndigoAssets.defaults.workspaceDir

enablePlugins(ScalaJSPlugin)

version := "0.1.0-SNAPSHOT"
scalaVersion := "3.6.4"
name := "scala-roguelike"

lazy val gameOptions: IndigoOptions =
  IndigoOptions.defaults
    .withWindowSize(1536, 768)
    .withAssetDirectory("assets")
    .excludeAssets {
      case p if p.endsWith(os.RelPath.rel / ".gitkeep") => true
      case p if p.segments.last.startsWith(".")          => true
      case _                                            => false
    }

lazy val root = (project in file("."))
  .settings(
    name := "scala-roguelike",
    indigoOptions := gameOptions,
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test,
      "io.indigoengine" %%% "indigo" % "0.22.0"
    ),
    // Basic coverage settings (limited support with ScalaJS)
    coverageMinimumStmtTotal := 49, // Set based on current measured coverage
    coverageFailOnMinimum := false, // Don't fail builds due to ScalaJS limitation
    coverageHighlighting := true,
    coverageExcludedPackages := "generated.*",
    Compile / sourceGenerators += Def.task {
      IndigoGenerators("generated")
        .embedFont(
          moduleName = "PixelFont",
          font = workspaceDir / "assets" / "fonts" / "Kenney Pixel.ttf",
          fontOptions = FontOptions(
            fontKey = "PixelFont",
            fontSize = 16,
            charSet = CharSet.ASCII,
            color = RGB.White,
            antiAlias = false,
            layout = FontLayout.normal
          ),
          imageOut = workspaceDir / "assets" / "generated"
        )
        .embedFont(
          moduleName = "PixelFontSmall",
          font = workspaceDir / "assets" / "fonts" / "Kenney Mini.ttf",
          fontOptions = FontOptions(
            fontKey = "PixelFontSmall",
            fontSize = 8,
            charSet = CharSet.ASCII,
            color = RGB.White,
            antiAlias = false,
            layout = FontLayout.normal
          ),
          imageOut = workspaceDir / "assets" / "generated"
        )
        .generateConfig("Config", gameOptions)
        .listAssets("Assets", gameOptions.assets)
        .toSourceFiles((Compile / sourceManaged).value) ++ {
          // Generate version info with current git commit
          val gitCommit = try {
            scala.sys.process.Process("git rev-parse --short HEAD").lineStream.headOption.getOrElse("unknown")
          } catch {
            case _: Exception => "unknown"
          }
          
          val versionFile = (Compile / sourceManaged).value / "generated" / "Version.scala"
          IO.write(versionFile, s"""package generated
                                  |
                                  |object Version {
                                  |  val gitCommit: String = "$gitCommit"
                                  |  val buildTime: String = "${java.time.Instant.now()}"
                                  |}
                                  |""".stripMargin)
          Seq(versionFile)
        }
    }
  )

addCommandAlias(
  "runGame",
  List(
    "compile",
    "fastLinkJS",
    "indigoRun"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "build",
  List(
    "compile",
    "fastLinkJS",
    "indigoBuild"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "testCoverage",
  List(
    "clean",
    "coverage",
    "test",
    "coverageReport"
  ).mkString(";", ";", "")
)
