package game

import map.{WorldMap, TileType, MapBounds}
import org.scalatest.funsuite.AnyFunSuite

class SafeSpawnTest extends AnyFunSuite {

  test("findSafeSpawn should return original point if safe") {
    val point = Point(0, 0)
    val tiles = Map(point -> TileType.Floor)
    val worldMap = WorldMap(
      tiles = tiles,
      dungeons = Seq.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = MapBounds(0, 0, 0, 0)
    )

    val safePoint = StartingState.findSafeSpawn(point, worldMap)
    assert(safePoint == point)
  }

  test("findSafeSpawn should find nearest safe point if start is unsafe") {
    val start = Point(0, 0)
    // (0,0) is Water (unsafe)
    // (1,0) is Wall (unsafe)
    // (0,1) is Dirt (safe)
    val tiles = Map(
      Point(0, 0) -> TileType.Water,
      Point(1, 0) -> TileType.Wall,
      Point(0, 1) -> TileType.Dirt
    )
    val worldMap = WorldMap(
      tiles = tiles,
      dungeons = Seq.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = MapBounds(0, 0, 0, 0)
    )

    val safePoint = StartingState.findSafeSpawn(start, worldMap)
    assert(safePoint == Point(0, 1))
  }

  test("findSafeSpawn should return start if no safe points found (fallback)") {
    val start = Point(0, 0)
    // All nearby are unsafe
    val tiles = Map(
      Point(0, 0) -> TileType.Water,
      Point(1, 0) -> TileType.Wall,
      Point(0, 1) -> TileType.Tree
    )
    val worldMap = WorldMap(
      tiles = tiles,
      dungeons = Seq.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = MapBounds(0, 0, 0, 0)
    )

    // Should return start because our mock map is tiny and spiral will exhaust quickly or find nothing
    // Actually our method sprials out to radius 50.
    // If map.get(p) returns None (for out of bounds in this mock), it maps to 'false' (unsafe).
    // So it should indeed return start as fallback.
    val safePoint = StartingState.findSafeSpawn(start, worldMap)
    assert(safePoint == start)
  }
}
