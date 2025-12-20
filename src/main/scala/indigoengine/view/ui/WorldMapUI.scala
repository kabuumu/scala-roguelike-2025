package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import _root_.ui.UIConfig.*
import _root_.ui.GameController
import map.TileType

object WorldMapUI {

  /** Generates a cached map view at world generation time for performance. This
    * uses CloneBatches to efficiently render thousands of tiles.
    *
    * @param worldTiles
    *   Map of tile positions to tile types
    * @param canvasWidth
    *   Width of the game canvas
    * @param canvasHeight
    *   Height of the game canvas
    * @return
    *   SceneUpdateFragment representing the entire world map
    */
  def worldMapView(
      worldTiles: Map[game.Point, map.TileType],
      canvasWidth: Int,
      canvasHeight: Int
  ): SceneUpdateFragment = {
    import map.TileType

    println(
      s"[WorldMap] Generating map with ${worldTiles.size} tiles, canvas: ${canvasWidth}x${canvasHeight}"
    )

    // Calculate bounds
    val allPositions = worldTiles.keys.toSeq
    if (allPositions.isEmpty) {
      throw new Exception("[WorldMap] No tiles available to render world map.")
    }

    val minX = allPositions.map(_.x).min
    val maxX = allPositions.map(_.x).max
    val minY = allPositions.map(_.y).min
    val maxY = allPositions.map(_.y).max

    val mapWidth = maxX - minX + 1
    val mapHeight = maxY - minY + 1

    val pixelSize = 1

    // Center the map on screen
    val mapPixelWidth = mapWidth * pixelSize
    val mapPixelHeight = mapHeight * pixelSize
    val offsetX = (canvasWidth - mapPixelWidth) / 2
    val offsetY = (canvasHeight - mapPixelHeight) / 2

    def getTileColor(tileType: TileType): RGBA = tileType match {
      case TileType.Floor | TileType.MaybeFloor =>
        RGBA.fromHexString("#847066") // Brown for dungeon/shop floor
      case TileType.Wall => RGBA.fromHexString("#352f2e") // Dark grey for walls
      case TileType.Water  => RGBA.fromHexString("#194f80") // Blue for water
      case TileType.Bridge => RGBA.fromHexString("#845425") // Brown for bridges
      case TileType.Rock   =>
        RGBA.fromHexString("#4f4240") // Lighter grey for rocks
      case TileType.Tree =>
        RGBA.fromHexString("#0f2c0c") // Dark green for trees
      case TileType.Grass1 | TileType.Grass2 | TileType.Grass3 =>
        RGBA.fromHexString("#399a4d") // Lighter green for grass
      case TileType.Dirt =>
        RGBA.fromHexString("#b28b78") // Brown for dirt/paths
    }

    // Group tiles by color to create batches
    val tilesByColor = worldTiles.groupBy { case (_, tileType) =>
      getTileColor(tileType)
    }

    // Create CloneIds and Blanks for each color
    val cloneData = tilesByColor.keys.map { color =>
      val id = CloneId(s"map_tile_${color.hashCode}")
      val blank = CloneBlank(
        id,
        Shape.Box(
          Rectangle(Point.zero, Size(pixelSize, pixelSize)),
          Fill.Color(color)
        )
      )
      (color, id, blank)
    }.toList

    val cloneBlanks = cloneData.map(_._3)
    val colorToId = cloneData.map(d => d._1 -> d._2).toMap

    // Create CloneBatches
    val batches = tilesByColor
      .map { case (color, tiles) =>
        val cloneId = colorToId(color)
        val transformData = tiles.map { case (pos, _) =>
          val x = offsetX + ((pos.x - minX) * pixelSize)
          val y = offsetY + ((pos.y - minY) * pixelSize)
          CloneBatchData(x, y)
        }.toSeq

        CloneBatch(cloneId, transformData.toBatch)
      }
      .toSeq
      .toBatch

    println(
      s"[WorldMap] Generated ${batches.length} render batches for ${worldTiles.size} tiles"
    )

    SceneUpdateFragment(
      Layer.Content(batches)
    ).addCloneBlanks(cloneBlanks.toBatch)
  }

  def render(model: GameController): SceneUpdateFragment = {
    val mapView = worldMapView(
      model.gameState.worldMap.tiles,
      canvasWidth,
      canvasHeight
    )

    // Add "Press any key to exit" message
    val exitMessage = UIUtils.text(
      "Press any key to exit",
      (canvasWidth - 160) / 2,
      canvasHeight - spriteScale * 2
    )

    mapView |+| SceneUpdateFragment(Layer.Content(exitMessage))
  }
}
