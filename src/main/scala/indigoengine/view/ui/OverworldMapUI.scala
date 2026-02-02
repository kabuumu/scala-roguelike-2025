package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import _root_.ui.UIConfig.*
import _root_.ui.GameController
import _root_.ui.UIState
import map.{OverworldMap, OverworldTileType}
import game.Point

/** UI component for rendering the zoomed-out overworld map preview. */
object OverworldMapUI {

  /** Pixel size for each overworld tile in the preview. */
  val pixelSize: Int = 2

  /** Renders the overworld map preview.
    *
    * @param overworldMap
    *   The generated overworld map
    * @param seed
    *   The seed used for generation (for display)
    * @return
    *   SceneUpdateFragment for the overworld preview
    */
  def render(overworldMap: OverworldMap, seed: Long): SceneUpdateFragment = {
    // Center the map on screen
    val mapPixelWidth = overworldMap.width * pixelSize
    val mapPixelHeight = overworldMap.height * pixelSize
    val offsetX = (canvasWidth - mapPixelWidth) / 2
    val offsetY =
      (canvasHeight - mapPixelHeight) / 2 - 20 // Leave room for text

    // Group tiles by color for efficient batch rendering
    val tilesByColor = overworldMap.tiles.groupBy { case (_, tileType) =>
      getTileColor(tileType)
    }

    // Create CloneIds and Blanks for each color
    val cloneData = tilesByColor.keys.map { color =>
      val id = CloneId(s"overworld_tile_${color.hashCode}")
      val blank = CloneBlank(
        id,
        Shape.Box(
          Rectangle(indigo.Point.zero, Size(pixelSize, pixelSize)),
          Fill.Color(color)
        )
      )
      (color, id, blank)
    }.toSeq

    val cloneBlanks = cloneData.map(_._3)

    // Create CloneBatches for all tiles
    val batches = cloneData
      .map { case (color, cloneId, _) =>
        val tiles = tilesByColor(color)
        val transformData = tiles.map { case (pos, _) =>
          val x = offsetX + pos.x * pixelSize
          val y = offsetY + pos.y * pixelSize
          CloneBatchData(x, y)
        }.toSeq

        CloneBatch(cloneId, transformData.toBatch)
      }
      .toSeq
      .toBatch

    // Add title and instructions
    val title = UIUtils.text(
      "WORLD MAP PREVIEW",
      (canvasWidth - 140) / 2,
      10
    )

    val seedText = UIUtils.text(
      s"Seed: $seed",
      (canvasWidth - 120) / 2,
      canvasHeight - 40
    )

    val instructions = UIUtils.text(
      "Press any key to return",
      (canvasWidth - 180) / 2,
      canvasHeight - 20
    )

    // Create legend
    val legendStartY = offsetY
    val legendX = offsetX + mapPixelWidth + 20
    val legendItems = Seq(
      ("Ocean", RGBA.fromHexString("#1a1a4d")),
      ("Water", RGBA.fromHexString("#3355aa")),
      ("Beach", RGBA.fromHexString("#d4c35c")),
      ("Plains", RGBA.fromHexString("#7cb342")),
      ("Forest", RGBA.fromHexString("#2d5a27")),
      ("Desert", RGBA.fromHexString("#c9a55c")),
      ("Mountain", RGBA.fromHexString("#6b6b6b")),
      ("Village", RGBA.White),
      ("Town", RGBA.Yellow),
      ("City", RGBA.Red),
      ("Road", RGBA.fromHexString("#8b7355")),
      ("Bridge", RGBA.fromHexString("#a0522d")),
      ("Path", RGBA.fromHexString("#b8a07a")),
      ("PathBridge", RGBA.fromHexString("#c9955c")),
      ("Trail", RGBA.fromHexString("#CCCCCC")),
      ("TrailBridge", RGBA.fromHexString("#888888"))
    )

    val legendElements = legendItems.zipWithIndex.flatMap {
      case ((name, color), idx) =>
        val y = legendStartY + idx * 14
        Seq(
          Shape.Box(
            Rectangle(indigo.Point(legendX, y), Size(10, 10)),
            Fill.Color(color)
          ),
          UIUtils.text(name, legendX + 14, y)
        )
    }

    SceneUpdateFragment(
      Layer.Content(
        batches ++
          Batch(title, seedText, instructions) ++
          legendElements.toBatch
      )
    ).addCloneBlanks(cloneBlanks.toBatch)
  }

  /** Returns the display color for each overworld tile type. */
  private def getTileColor(tileType: OverworldTileType): RGBA = tileType match {
    case OverworldTileType.Ocean  => RGBA.fromHexString("#1a1a4d") // Deep blue
    case OverworldTileType.Water  => RGBA.fromHexString("#3355aa") // Blue
    case OverworldTileType.Beach  => RGBA.fromHexString("#d4c35c") // Sandy
    case OverworldTileType.Plains =>
      RGBA.fromHexString("#7cb342") // Light green
    case OverworldTileType.Forest => RGBA.fromHexString("#2d5a27") // Dark green
    case OverworldTileType.Desert => RGBA.fromHexString("#c9a55c") // Tan
    case OverworldTileType.Mountain => RGBA.fromHexString("#6b6b6b") // Gray
    case OverworldTileType.Village  => RGBA.White // White pixel
    case OverworldTileType.Town     => RGBA.Yellow // Yellow pixel
    case OverworldTileType.City     => RGBA.Red // Red pixel
    case OverworldTileType.Road   => RGBA.fromHexString("#8b7355") // Brown road
    case OverworldTileType.Bridge =>
      RGBA.fromHexString("#a0522d") // Dark brown bridge
    case OverworldTileType.Path =>
      RGBA.fromHexString("#b8a07a") // Light tan path
    case OverworldTileType.PathBridge =>
      RGBA.fromHexString("#c9955c") // Light brown path bridge
    case OverworldTileType.Trail =>
      RGBA.fromHexString("#CCCCCC") // Light gray trail
    case OverworldTileType.TrailBridge =>
      RGBA.fromHexString("#888888") // Dark gray trail bridge
  }
}
