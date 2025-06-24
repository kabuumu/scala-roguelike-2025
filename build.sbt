import indigoplugin.*
import indigoplugin.IndigoAssets.defaults.workspaceDir

enablePlugins(ScalaJSPlugin)

version := "0.1.0-SNAPSHOT"
scalaVersion := "3.6.4"
name := "scala-roguelike"

lazy val gameOptions: IndigoOptions =
  IndigoOptions.defaults
    .withAssetDirectory("assets")
    .excludeAssets {
      case p if p.endsWith(os.RelPath.rel / ".gitkeep") => true
      case _                                            => false
    }

lazy val root = (project in file("."))
  .settings(
    name := "scala-roguelike",
    indigoOptions := gameOptions,
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test,
      "io.indigoengine" %%% "indigo" % "0.21.2-SNAPSHOT"
    ),
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
        .toSourceFiles((Compile / sourceManaged).value)
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
