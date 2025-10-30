package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class PathAroundObstaclesTest extends AnyFunSuite {
  
  test("generatePathAroundObstacles avoids obstacle tiles") {
    val bounds = MapBounds(0, 10, 0, 10)
    val start = Point(10, 50)
    val target = Point(90, 50)
    
    // Create a wall of obstacles in the middle
    val obstacles = (40 to 60).map(x => Point(x, 50)).toSet
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, obstacles, width = 0, bounds)
    
    assert(path.nonEmpty, "Path should be generated")
    assert(path.contains(start), "Path should contain start point")
    assert(path.contains(target), "Path should contain target point")
    
    // Verify path doesn't go through obstacles
    val pathIntersectsObstacles = path.intersect(obstacles)
    assert(pathIntersectsObstacles.isEmpty, 
      s"Path should not intersect obstacles, but found ${pathIntersectsObstacles.size} intersections")
    
    println(s"Generated path with ${path.size} tiles avoiding ${obstacles.size} obstacles")
  }
  
  test("generatePathAroundObstacles navigates around dungeon-like structure") {
    val bounds = MapBounds(0, 20, 0, 20)
    val start = Point(10, 10)
    val target = Point(190, 190)
    
    // Create a box-shaped obstacle (simulating a dungeon room)
    val boxObstacles = (for {
      x <- 80 to 120
      y <- 80 to 120
      if x == 80 || x == 120 || y == 80 || y == 120 // Only walls, not interior
    } yield Point(x, y)).toSet
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, boxObstacles, width = 0, bounds)
    
    assert(path.nonEmpty, "Path should be generated around box obstacle")
    assert(path.contains(start), "Path should contain start")
    assert(path.contains(target), "Path should contain target")
    
    // Verify no intersection with walls
    val intersections = path.intersect(boxObstacles)
    assert(intersections.isEmpty, 
      s"Path should go around the box, not through it. Found ${intersections.size} wall intersections")
    
    println(s"Successfully navigated around box obstacle with path of ${path.size} tiles")
  }
  
  test("generatePathAroundObstacles with width avoids obstacles in widened area") {
    val bounds = MapBounds(0, 10, 0, 10)
    val start = Point(10, 30)
    val target = Point(90, 30)
    
    // Single obstacle in the middle
    val obstacles = Set(Point(50, 30))
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, obstacles, width = 1, bounds)
    
    assert(path.nonEmpty, "Path should be generated")
    
    // With width=1, the path should avoid the obstacle even when widened
    val intersections = path.intersect(obstacles)
    assert(intersections.isEmpty, 
      s"Widened path should not intersect obstacle: ${intersections}")
    
    println(s"Wide path (${path.size} tiles) successfully avoids obstacles")
  }
  
  test("generatePathAroundObstacles falls back to direct line if no path exists") {
    val bounds = MapBounds(0, 5, 0, 5)
    val start = Point(10, 25)
    val target = Point(40, 25)
    
    // Create obstacles that completely block the path
    val obstacles = (0 to 50).flatMap { x =>
      Seq(Point(x, 20), Point(x, 30))
    }.toSet
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, obstacles, width = 0, bounds)
    
    // Should still generate some path (falls back to direct line)
    assert(path.nonEmpty, "Should generate fallback path even when blocked")
    assert(path.contains(start), "Fallback path contains start")
    assert(path.contains(target), "Fallback path contains target")
    
    println(s"Fallback path generated with ${path.size} tiles when no clear route exists")
  }
  
  test("generatePathAroundObstacles respects map bounds") {
    val bounds = MapBounds(0, 5, 0, 5)
    val start = Point(10, 10)
    val target = Point(50, 50)
    
    val obstacles = Set(Point(30, 30))
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, obstacles, width = 0, bounds)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    // All path tiles should be within bounds
    path.foreach { point =>
      assert(point.x >= tileMinX && point.x <= tileMaxX, 
        s"Path point ${point.x} outside X bounds [$tileMinX, $tileMaxX]")
      assert(point.y >= tileMinY && point.y <= tileMaxY,
        s"Path point ${point.y} outside Y bounds [$tileMinY, $tileMaxY]")
    }
    
    println(s"All ${path.size} path tiles within bounds")
  }
  
  test("path to dungeon entrance avoids cutting through dungeon rooms") {
    val bounds = MapBounds(-10, 10, -10, 10)
    
    // Simulate a dungeon structure with multiple rooms
    val dungeonBounds = MapBounds(-5, 5, -5, 0)
    val dungeonConfig = DungeonConfig(
      bounds = Some(dungeonBounds),
      size = 8,
      seed = 12345
    )
    
    val dungeon = MapGenerator.generateDungeon(dungeonConfig)
    
    // Player spawn point outside dungeon
    val playerStart = Point(-80, 80) // Far from dungeon
    
    // Find an accessible walkable tile in the starting room (like StartingState does)
    val startRoomX = dungeon.startPoint.x * Dungeon.roomSize
    val startRoomY = dungeon.startPoint.y * Dungeon.roomSize
    
    val startRoomTiles = (for {
      x <- startRoomX to (startRoomX + Dungeon.roomSize)
      y <- startRoomY to (startRoomY + Dungeon.roomSize)
    } yield Point(x, y)).toSet
    
    val walkableTiles = startRoomTiles.filterNot { point =>
      dungeon.walls.contains(point) || dungeon.rocks.contains(point) || dungeon.water.contains(point)
    }
    
    val centerPoint = Point(
      startRoomX + Dungeon.roomSize / 2,
      startRoomY + Dungeon.roomSize / 2
    )
    
    val dungeonEntrance = if (walkableTiles.contains(centerPoint)) {
      centerPoint
    } else {
      walkableTiles.minBy { tile =>
        val dx = tile.x - centerPoint.x
        val dy = tile.y - centerPoint.y
        dx * dx + dy * dy
      }
    }
    
    // Get ALL dungeon tiles except a small entrance area as obstacles
    // This prevents the path from cutting through ANY part of the dungeon
    // but allows connection at the entrance point
    val entranceArea = (for {
      dx <- -2 to 2
      dy <- -2 to 2
    } yield Point(dungeonEntrance.x + dx, dungeonEntrance.y + dy)).toSet
    
    val dungeonObstacles = dungeon.tiles.keySet.diff(entranceArea)
    
    println(s"Player at $playerStart, dungeon entrance at $dungeonEntrance")
    println(s"Starting room has ${walkableTiles.size} walkable tiles")
    println(s"Dungeon total tiles: ${dungeon.tiles.size}")
    println(s"Dungeon obstacles (all tiles except starting room): ${dungeonObstacles.size}")
    
    // Verify the entrance point is actually walkable
    assert(!dungeonObstacles.contains(dungeonEntrance),
      s"Dungeon entrance $dungeonEntrance should not be an obstacle")
    assert(walkableTiles.contains(dungeonEntrance),
      s"Dungeon entrance $dungeonEntrance should be walkable")
    
    // Generate path that avoids dungeon obstacles
    val path = PathGenerator.generatePathAroundObstacles(
      playerStart,
      dungeonEntrance,
      dungeonObstacles,
      width = 1,
      bounds
    )
    
    assert(path.nonEmpty, "Path should be generated to dungeon")
    
    // CRITICAL: Verify path doesn't cut through ANY dungeon tiles (floors, walls, etc.)
    val allDungeonTilesExceptStart = dungeon.tiles.keySet.diff(startRoomTiles)
    val dungeonIntersections = path.intersect(allDungeonTilesExceptStart)
    assert(dungeonIntersections.isEmpty,
      s"Path should not cut through ANY dungeon tiles (floors, walls, etc.). Found ${dungeonIntersections.size} intersections at: ${dungeonIntersections.take(5)}")
    
    // Also verify path doesn't intersect obstacles specifically
    val obstacleIntersections = path.intersect(dungeonObstacles)
    assert(obstacleIntersections.isEmpty,
      s"Path should not intersect obstacles. Found ${obstacleIntersections.size} intersections")
    
    // Verify path only enters dungeon at the starting room
    val pathInDungeon = path.intersect(dungeon.tiles.keySet)
    val pathInStartRoom = pathInDungeon.intersect(startRoomTiles)
    assert(pathInDungeon == pathInStartRoom,
      s"Path should only enter dungeon at starting room. Path in dungeon: ${pathInDungeon.size}, Path in start room: ${pathInStartRoom.size}")
    
    println(s"✓ Dungeon entrance is accessible (walkable)")
    println(s"✓ Path successfully navigates around dungeon (${path.size} tiles)")
    println(s"✓ No intersections with ${dungeonObstacles.size} dungeon obstacle tiles")
    println(s"✓ Path only enters dungeon at starting room (${pathInStartRoom.size} tiles in start room)")
    println(s"✓ Path does NOT cut through dungeon floors, walls, or other rooms")
  }
}

