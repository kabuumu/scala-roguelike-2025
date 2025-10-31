package map

import game.{Direction, Point}
import org.scalatest.funsuite.AnyFunSuite

class MapConfigTest extends AnyFunSuite {
  
  test("MapBounds validates requirements") {
    assertThrows[IllegalArgumentException] {
      MapBounds(10, 5, 0, 10) // minX > maxX
    }
    
    assertThrows[IllegalArgumentException] {
      MapBounds(0, 10, 10, 5) // minY > maxY
    }
    
    // Valid bounds should not throw
    val bounds = MapBounds(-5, 5, -5, 5)
    assert(bounds.roomWidth == 11)
    assert(bounds.roomHeight == 11)
  }
  
  test("MapBounds calculates dimensions correctly") {
    val bounds = MapBounds(-2, 2, -3, 3)
    
    assert(bounds.roomWidth == 5, "Width should be 5 rooms")
    assert(bounds.roomHeight == 7, "Height should be 7 rooms")
    assert(bounds.roomArea == 35, "Area should be 35 rooms")
  }
  
  test("MapBounds.contains checks if point is within bounds") {
    val bounds = MapBounds(0, 5, 0, 5)
    
    assert(bounds.contains(Point(0, 0)))
    assert(bounds.contains(Point(5, 5)))
    assert(bounds.contains(Point(2, 3)))
    
    assert(!bounds.contains(Point(-1, 0)))
    assert(!bounds.contains(Point(6, 0)))
    assert(!bounds.contains(Point(0, -1)))
    assert(!bounds.contains(Point(0, 6)))
  }
  
  test("MapBounds.toTileBounds converts room to tile coordinates") {
    val bounds = MapBounds(0, 2, 0, 2)
    val (minX, maxX, minY, maxY) = bounds.toTileBounds(roomSize = 10)
    
    assert(minX == 0)
    assert(maxX == 30) // (2 + 1) * 10
    assert(minY == 0)
    assert(maxY == 30)
  }
  
  test("MapBounds.fromTiles creates bounds from tile coordinates") {
    val bounds = MapBounds.fromTiles(0, 30, 0, 30, roomSize = 10)
    
    assert(bounds.minRoomX == 0)
    assert(bounds.maxRoomX == 3)
    assert(bounds.minRoomY == 0)
    assert(bounds.maxRoomY == 3)
  }
  
  test("MapBounds.centered creates centered bounds") {
    val bounds = MapBounds.centered(width = 4, height = 4, centerX = 0, centerY = 0)
    
    assert(bounds.minRoomX == -2)
    assert(bounds.maxRoomX == 1)
    assert(bounds.minRoomY == -2)
    assert(bounds.maxRoomY == 1)
    assert(bounds.roomWidth == 4)
    assert(bounds.roomHeight == 4)
  }
  
  test("MapBounds.describe provides human-readable output") {
    val bounds = MapBounds(-2, 2, -3, 3)
    val description = bounds.describe
    
    assert(description.contains("(-2,-3)"))
    assert(description.contains("(2,3)"))
    assert(description.contains("5x7"))
    assert(description.contains("35"))
    
    println(s"Bounds description: $description")
  }
  
  test("DungeonConfig auto-calculates size, doors, and items from bounds") {
    val bounds = MapBounds(-5, 5, -5, 5)  // 11x11 = 121 area
    val config = DungeonConfig(bounds = bounds, seed = 1)
    
    // Size should be ~7% of 121 = ~8, clamped between 5 and 10
    assert(config.size == 8)
    // Locked doors: size / 20 = 8 / 20 = 0
    assert(config.lockedDoorCount == 0)
    // Items: max(1, size / 6) = max(1, 1) = 1
    assert(config.itemCount == 1)
    // Entrance side defaults to Down
    assert(config.entranceSide == Direction.Down)
  }
  
  test("DungeonConfig.isWithinBounds checks bounds correctly") {
    val bounds = MapBounds(-5, 5, -5, 5)
    val config = DungeonConfig(bounds = bounds, seed = 1)
    
    assert(config.isWithinBounds(Point(0, 0)))
    assert(config.isWithinBounds(Point(5, 5)))
    assert(config.isWithinBounds(Point(-5, -5)))
    
    assert(!config.isWithinBounds(Point(6, 0)))
    assert(!config.isWithinBounds(Point(0, 6)))
    assert(!config.isWithinBounds(Point(-6, 0)))
  }
  
  test("DungeonConfig.getEntranceRoom positions entrance correctly") {
    val bounds = MapBounds(-4, 4, -4, 4) // 9x9 grid centered at origin
    
    val config = DungeonConfig(bounds = bounds, seed = 1)
    val entrance = config.getEntranceRoom
    
    // Entrance side is now fixed to Down
    assert(entrance.y == 4, s"Down entrance should be at maxY, got $entrance")
    assert(entrance.x == 0, s"Down entrance should be centered in X, got $entrance")
    
    println(s"Entrance position (fixed to Down side): $entrance")
  }
  
  test("WorldConfig validates density requirements") {
    assertThrows[IllegalArgumentException] {
      WorldConfig(MapBounds(0, 10, 0, 10), grassDensity = 1.5) // > 1.0
    }
    
    assertThrows[IllegalArgumentException] {
      WorldConfig(MapBounds(0, 10, 0, 10), grassDensity = -0.1) // < 0.0
    }
    
    assertThrows[IllegalArgumentException] {
      WorldConfig(MapBounds(0, 10, 0, 10), grassDensity = 0.6, treeDensity = 0.3, dirtDensity = 0.3) // sum > 1.0
    }
    
    // Valid config should not throw
    val config = WorldConfig(MapBounds(0, 10, 0, 10), grassDensity = 0.7, treeDensity = 0.2, dirtDensity = 0.1)
    assert(config.grassDensity == 0.7)
  }
  
  test("WorldConfig default values") {
    val bounds = MapBounds(0, 10, 0, 10)
    val config = WorldConfig(bounds)
    
    assert(config.grassDensity == 0.7)
    assert(config.treeDensity == 0.15)
    assert(config.dirtDensity == 0.1)
    assert(config.ensureWalkablePaths)
    assert(config.perimeterTrees)
  }
  
  test("Configuration classes are AI-readable") {
    val bounds = MapBounds(-3, 3, -2, 2)
    val dungeonConfig = DungeonConfig(
      bounds = bounds,
      seed = 1
    )
    val worldConfig = WorldConfig(
      bounds = bounds,
      grassDensity = 0.6,
      treeDensity = 0.2,
      dirtDensity = 0.15
    )
    
    println("\n=== AI-Readable Configuration Output ===")
    println(s"Bounds: ${bounds.describe}")
    println(s"DungeonConfig: bounds=${dungeonConfig.bounds.describe}, " +
            s"entranceSide=${dungeonConfig.entranceSide}, size=${dungeonConfig.size} (auto-calc), " +
            s"lockedDoors=${dungeonConfig.lockedDoorCount} (auto-calc), items=${dungeonConfig.itemCount} (auto-calc)")
    println(s"Entrance Room: ${dungeonConfig.getEntranceRoom}")
    println(s"WorldConfig: grass=${worldConfig.grassDensity}, tree=${worldConfig.treeDensity}, " +
            s"dirt=${worldConfig.dirtDensity}, walkablePaths=${worldConfig.ensureWalkablePaths}, " +
            s"perimeterTrees=${worldConfig.perimeterTrees}")
    println("=========================================\n")
    
    // Verify output is present
    assert(bounds.describe.nonEmpty)
  }
}
