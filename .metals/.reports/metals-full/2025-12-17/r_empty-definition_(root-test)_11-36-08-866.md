error id: file://<WORKSPACE>/src/test/scala/map/PathGeneratorTest.scala:`<none>`.
file://<WORKSPACE>/src/test/scala/map/PathGeneratorTest.scala
empty definition using pc, found symbol in pc: `<none>`.
semanticdb not found
empty definition using fallback
non-local guesses:
	 -PathGenerator.generatePath.
	 -PathGenerator.generatePath#
	 -PathGenerator.generatePath().
	 -scala/Predef.PathGenerator.generatePath.
	 -scala/Predef.PathGenerator.generatePath#
	 -scala/Predef.PathGenerator.generatePath().
offset: 324
uri: file://<WORKSPACE>/src/test/scala/map/PathGeneratorTest.scala
text:
```scala
package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class PathGeneratorTest extends AnyFunSuite {
  
  test("generatePath creates path from start to target") {
    val bounds = MapBounds(0, 10, 0, 10)
    val start = Point(10, 10)
    val target = Point(90, 90)
    
    val path = PathGenerator.genera@@tePath(start, target, width = 0, bounds)
    
    assert(path.nonEmpty, "Path should have tiles")
    assert(path.contains(start), "Path should contain start point")
    assert(path.contains(target), "Path should contain target point")
    
    println(s"Generated path with ${path.size} tiles from $start to $target")
  }
  
  test("generatePath respects bounds") {
    val bounds = MapBounds(0, 5, 0, 5)
    val start = Point(0, 0)
    val target = Point(55, 55)
    
    val path = PathGenerator.generatePath(start, target, width = 0, bounds)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    path.foreach { point =>
      assert(point.x >= tileMinX && point.x <= tileMaxX, s"X coordinate ${point.x} out of bounds")
      assert(point.y >= tileMinY && point.y <= tileMaxY, s"Y coordinate ${point.y} out of bounds")
    }
    
    println(s"All ${path.size} path tiles within bounds")
  }
  
  test("generatePath with width creates wider paths") {
    val bounds = MapBounds(0, 10, 0, 10)
    val start = Point(10, 10)
    val target = Point(90, 90)
    
    val narrowPath = PathGenerator.generatePath(start, target, width = 0, bounds)
    val widePath = PathGenerator.generatePath(start, target, width = 2, bounds)
    
    assert(widePath.size > narrowPath.size,
           s"Wide path should have more tiles: wide=${widePath.size}, narrow=${narrowPath.size}")
    
    println(s"Narrow path: ${narrowPath.size} tiles, Wide path: ${widePath.size} tiles")
  }
  
  test("generateConvergingPaths creates multiple paths to single target") {
    val bounds = MapBounds(0, 10, 0, 10)
    val startPoints = Seq(
      Point(10, 10),
      Point(100, 10),
      Point(10, 100)
    )
    val target = Point(50, 50)
    
    val paths = PathGenerator.generateConvergingPaths(startPoints, target, width = 0, bounds)
    
    assert(paths.nonEmpty, "Should generate path tiles")
    assert(paths.contains(target), "All paths should reach the target")
    
    // All start points should be in the combined path set
    startPoints.foreach { start =>
      assert(paths.contains(start), s"Path should contain start point $start")
    }
    
    println(s"Generated ${paths.size} tiles from ${startPoints.size} converging paths")
  }
  
  test("generateDungeonPaths creates paths leading to dungeon entrances") {
    val bounds = MapBounds(0, 10, 0, 10)
    val dungeonEntrances = Seq(
      Point(50, 50),
      Point(80, 80)
    )
    
    val paths = PathGenerator.generateDungeonPaths(
      dungeonEntrances,
      bounds,
      pathsPerEntrance = 2,
      width = 1,
      seed = 12345
    )
    
    assert(paths.nonEmpty, "Should generate path tiles")
    
    // All dungeon entrances should be in the path set
    dungeonEntrances.foreach { entrance =>
      assert(paths.contains(entrance), s"Paths should reach entrance at $entrance")
    }
    
    println(s"Generated ${paths.size} path tiles leading to ${dungeonEntrances.size} dungeons")
  }
  
  test("describePaths provides AI-readable output") {
    val bounds = MapBounds(0, 5, 0, 5)
    val start = Point(10, 10)
    val target = Point(50, 50)
    
    val path = PathGenerator.generatePath(start, target, width = 1, bounds)
    val description = PathGenerator.describePaths(path, bounds)
    
    println("\n" + description + "\n")
    
    assert(description.contains("Path Generation Summary"))
    assert(description.contains("Total path tiles"))
    assert(description.contains("Coverage"))
  }
  
  test("path connects start and target in a reasonable line") {
    val bounds = MapBounds(0, 10, 0, 10)
    val start = Point(10, 10)
    val target = Point(90, 90)
    
    val path = PathGenerator.generatePath(start, target, width = 0, bounds)
    
    // Path should have a reasonable number of tiles (not excessive detours)
    val manhattanDistance = math.abs(target.x - start.x) + math.abs(target.y - start.y)
    val diagonalDistance = math.max(math.abs(target.x - start.x), math.abs(target.y - start.y))
    
    // Bresenham's algorithm should create a path approximately diagonal distance long
    assert(path.size >= diagonalDistance, s"Path too short: ${path.size} < $diagonalDistance")
    assert(path.size <= manhattanDistance * 1.5, s"Path too long: ${path.size} > ${manhattanDistance * 1.5}")
    
    println(s"Path length: ${path.size} tiles (diagonal: $diagonalDistance, manhattan: $manhattanDistance)")
  }
  
  test("paths are deterministic with same parameters") {
    val bounds = MapBounds(0, 10, 0, 10)
    val dungeonEntrances = Seq(Point(50, 50))
    
    val paths1 = PathGenerator.generateDungeonPaths(dungeonEntrances, bounds, 2, 1, 99999)
    val paths2 = PathGenerator.generateDungeonPaths(dungeonEntrances, bounds, 2, 1, 99999)
    
    assert(paths1 == paths2, "Same parameters should produce identical paths")
    println(s"Deterministic generation verified: ${paths1.size} tiles")
  }
  
  test("paths produce different results with different seeds") {
    val bounds = MapBounds(0, 10, 0, 10)
    val dungeonEntrances = Seq(Point(50, 50))
    
    val paths1 = PathGenerator.generateDungeonPaths(dungeonEntrances, bounds, 2, 1, 11111)
    val paths2 = PathGenerator.generateDungeonPaths(dungeonEntrances, bounds, 2, 1, 22222)
    
    // Different seeds should produce different paths
    assert(paths1 != paths2, "Different seeds should produce different paths")
    println(s"Different seeds: paths1=${paths1.size} tiles, paths2=${paths2.size} tiles")
  }
  
  test("single path width creates connected line") {
    val bounds = MapBounds(0, 10, 0, 10)
    val start = Point(10, 10)
    val target = Point(50, 50)
    
    val path = PathGenerator.generatePath(start, target, width = 0, bounds)
    
    // Verify path forms a connected line (each point should have at least one neighbor in the path)
    val connectedCount = path.count { point =>
      val neighbors = Seq(
        Point(point.x + 1, point.y),
        Point(point.x - 1, point.y),
        Point(point.x, point.y + 1),
        Point(point.x, point.y - 1),
        Point(point.x + 1, point.y + 1),
        Point(point.x - 1, point.y - 1),
        Point(point.x + 1, point.y - 1),
        Point(point.x - 1, point.y + 1)
      )
      neighbors.exists(path.contains) || point == start || point == target
    }
    
    assert(connectedCount == path.size, "All path points should be connected")
    println(s"Path is fully connected: ${path.size} tiles")
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.