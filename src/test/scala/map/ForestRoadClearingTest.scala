package map

import org.scalatest.funsuite.AnyFunSuite
import game.Point

class ForestRoadClearingTest extends AnyFunSuite {

  test("ChunkManager preserves paths and bridges in forest chunks without spawning trees on roads") {
    val seed = 42L
    val bounds = MapBounds(-10, 10, -10, 10)

    // Create road points inside forest
    val pathPoints = (0 to 10).map(y => Point(0, y)).toSet
    val bridgePoints = Set(Point(0, 5))

    val baseWorldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      villages = Seq.empty,
      paths = pathPoints,
      bridges = bridgePoints,
      bounds = bounds,
      seed = seed
    )

    val worldConfig = WorldConfig(bounds = bounds, seed = seed)
    val worldMapWithChunks = ChunkManager.updateChunks(
      Point(0, 0),
      baseWorldMap,
      worldConfig,
      seed
    )

    // Verify no path or bridge point is rendered as a Tree or Wall
    pathPoints.foreach { pt =>
      val tile = worldMapWithChunks.getTile(pt)
      assert(tile.nonEmpty, s"Tile at $pt should be present")
      assert(tile.get != TileType.Tree, s"Path point $pt must not be rendered as a Tree")
      assert(tile.get != TileType.Wall && tile.get != TileType.Rock, s"Path point $pt must be walkable")
    }

    // Verify bridge point is Bridge
    assert(worldMapWithChunks.getTile(Point(0, 5)).contains(TileType.Bridge), "Bridge point must be Bridge")
  }
}
