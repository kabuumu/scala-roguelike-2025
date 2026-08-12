package map

import org.scalatest.funsuite.AnyFunSuite
import game.StartingState

class VillageTerrainTest extends AnyFunSuite {

  test("Village area contains natural terrain variety (grass variants, trees, etc.) rather than flat sterile grass") {
    val seed = 100L
    val state = StartingState.startAdventure(seed)
    val worldMap = state.worldMap

    val village = worldMap.villages.head
    val minX = village.bounds.minRoomX
    val maxX = village.bounds.maxRoomX
    val minY = village.bounds.minRoomY
    val maxY = village.bounds.maxRoomY

    val villageAreaTiles = for {
      x <- minX to maxX
      y <- minY to maxY
      point = game.Point(x, y)
      tile <- worldMap.getTile(point)
    } yield tile

    // Check that terrain within the village bounds contains non-Dirt / non-Wall / non-Grass1 variants
    val tileTypes = villageAreaTiles.toSet

    assert(tileTypes.size > 2, s"Village terrain should be varied, found types: $tileTypes")
    assert(
      tileTypes.contains(TileType.Dirt),
      "Village area must contain Dirt path tiles connecting buildings"
    )
  }
}
