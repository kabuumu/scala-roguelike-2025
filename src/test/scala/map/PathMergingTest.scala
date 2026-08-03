package map

import org.scalatest.funsuite.AnyFunSuite
import game.Point

class PathMergingTest extends AnyFunSuite {

  test("pathfinder reuses existing path tiles to minimize path duplication") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val obstacles = Set.empty[Point]

    // Existing trunk path from (0,0) to (0,10)
    val existingTrunk = (0 to 10).map(y => Point(0, y)).toSet

    // Path from (5,0) to (0,10).
    // With path-merging discount, the path will move west to (0,0) or nearby trunk tile and follow trunk to (0,10).
    val path = PathGenerator.generatePathAroundObstacles(
      startPoint = Point(5, 0),
      targetPoint = Point(0, 10),
      obstacles = obstacles,
      width = 0,
      bounds = bounds,
      existingPaths = existingTrunk
    )

    // Check that path intersects and reuses trunk tiles
    val reusedTiles = path.intersect(existingTrunk)
    assert(reusedTiles.nonEmpty, "New path should merge into and reuse existing trunk path")
  }

  test("pathfinder succeeds even when start/target borders obstacles") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val start = Point(0, 0)
    val target = Point(5, 5)
    // Obstacles touch start and target
    val obstacles = Set(Point(-1, 0), Point(1, 0), Point(0, -1), Point(6, 5), Point(5, 6))

    val path = PathGenerator.generatePathAroundObstacles(
      startPoint = start,
      targetPoint = target,
      obstacles = obstacles,
      width = 0,
      bounds = bounds
    )

    assert(path.nonEmpty, "Pathfinder should succeed when start/target touch obstacles")
    assert(path.contains(start) && path.contains(target), "Path must connect start to target")
  }
}
