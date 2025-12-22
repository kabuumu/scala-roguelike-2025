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
    * @param playerPosition
    *   Current position of the player (in grid coordinates)
    * @param seenPoints
    *   Set of points the player has visited/seen
    * @return
    *   SceneUpdateFragment representing the visible world map
    */
  def worldMapView(
      worldTiles: Map[game.Point, map.TileType],
      canvasWidth: Int,
      canvasHeight: Int,
      playerPosition: game.Point,
      seenPoints: Set[game.Point]
  ): SceneUpdateFragment = {
    import map.TileType

    // Filter tiles: only include those relevant to the map that have been seen
    val visibleTiles = worldTiles.filter { case (pos, _) =>
      seenPoints.contains(pos)
    }

    if (visibleTiles.isEmpty) {
      // Return empty fragment if nothing seen yet (shouldn't happen if player is somewhere)
      return SceneUpdateFragment.empty
    }

    val pixelSize = 2 // Increased pixel size for better visibility

    // Calculate offsets to center the player
    // playerPosition * pixelSize gives the player's location in "map pixels"
    // We want that location to be at (canvasWidth / 2, canvasHeight / 2)
    val centerX = canvasWidth / 2
    val centerY = canvasHeight / 2

    val offsetX = centerX - (playerPosition.x * pixelSize)
    val offsetY = centerY - (playerPosition.y * pixelSize)

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
    val tilesByColor = visibleTiles.groupBy { case (_, tileType) =>
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
          val x = offsetX + (pos.x * pixelSize)
          val y = offsetY + (pos.y * pixelSize)
          CloneBatchData(x, y)
        }.toSeq

        CloneBatch(cloneId, transformData.toBatch)
      }
      .toSeq
      .toBatch

    SceneUpdateFragment(
      Layer.Content(batches)
    ).addCloneBlanks(cloneBlanks.toBatch)
  }

  def render(model: GameController): SceneUpdateFragment = {
    val player = model.gameState.playerEntity
    val playerPos = player
      .get[game.entity.Movement]
      .map(_.position)
      .getOrElse(game.Point(0, 0))
    val sightMemory = player.get[game.entity.SightMemory]

    // Default to empty set if no sight memory component (shouldn't happen for player)
    val seenPoints = sightMemory.map(_.seenPoints).getOrElse(Set.empty)

    // Also include current field of view in "seen" points for the map
    // (Optional, but generally map shows what you can see right now too)
    // For now assuming existing unseen/seen logic in SightMemory handles this persistence.

    val mapView = worldMapView(
      model.gameState.worldMap.tiles,
      canvasWidth,
      canvasHeight,
      playerPos,
      seenPoints
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
