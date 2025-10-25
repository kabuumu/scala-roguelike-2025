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
  
  test("DungeonConfig default values") {
    val config = DungeonConfig()
    
    assert(config.bounds.isEmpty)
    assert(config.entranceSide == Direction.Up)
    assert(config.size == 10)
    assert(config.lockedDoorCount == 0)
    assert(config.itemCount == 0)
  }
  
  test("DungeonConfig.isWithinBounds with no bounds returns true") {
    val config = DungeonConfig()
    
    assert(config.isWithinBounds(Point(0, 0)))
    assert(config.isWithinBounds(Point(100, 100)))
    assert(config.isWithinBounds(Point(-100, -100)))
  }
  
  test("DungeonConfig.isWithinBounds checks bounds correctly") {
    val bounds = MapBounds(-5, 5, -5, 5)
    val config = DungeonConfig(bounds = Some(bounds))
    
    assert(config.isWithinBounds(Point(0, 0)))
    assert(config.isWithinBounds(Point(5, 5)))
    assert(config.isWithinBounds(Point(-5, -5)))
    
    assert(!config.isWithinBounds(Point(6, 0)))
    assert(!config.isWithinBounds(Point(0, 6)))
    assert(!config.isWithinBounds(Point(-6, 0)))
  }
  
  test("DungeonConfig.getEntranceRoom with no bounds") {
    val config = DungeonConfig()
    val entrance = config.getEntranceRoom
    
    assert(entrance == Point(0, 0))
  }
  
  test("DungeonConfig.getEntranceRoom positions entrance on correct side") {
    val bounds = MapBounds(-4, 4, -4, 4) // 9x9 grid centered at origin
    
    val upConfig = DungeonConfig(bounds = Some(bounds), entranceSide = Direction.Up)
    val upEntrance = upConfig.getEntranceRoom
    assert(upEntrance.y == -4, s"Up entrance should be at minY, got $upEntrance")
    assert(upEntrance.x == 0, s"Up entrance should be centered in X, got $upEntrance")
    
    val downConfig = DungeonConfig(bounds = Some(bounds), entranceSide = Direction.Down)
    val downEntrance = downConfig.getEntranceRoom
    assert(downEntrance.y == 4, s"Down entrance should be at maxY, got $downEntrance")
    assert(downEntrance.x == 0, s"Down entrance should be centered in X, got $downEntrance")
    
    val leftConfig = DungeonConfig(bounds = Some(bounds), entranceSide = Direction.Left)
    val leftEntrance = leftConfig.getEntranceRoom
    assert(leftEntrance.x == -4, s"Left entrance should be at minX, got $leftEntrance")
    assert(leftEntrance.y == 0, s"Left entrance should be centered in Y, got $leftEntrance")
    
    val rightConfig = DungeonConfig(bounds = Some(bounds), entranceSide = Direction.Right)
    val rightEntrance = rightConfig.getEntranceRoom
    assert(rightEntrance.x == 4, s"Right entrance should be at maxX, got $rightEntrance")
    assert(rightEntrance.y == 0, s"Right entrance should be centered in Y, got $rightEntrance")
    
    println(s"Entrance positions: Up=$upEntrance, Down=$downEntrance, Left=$leftEntrance, Right=$rightEntrance")
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
      bounds = Some(bounds),
      entranceSide = Direction.Down,
      size = 15,
      lockedDoorCount = 2,
      itemCount = 5
    )
    val worldConfig = WorldConfig(
      bounds = bounds,
      grassDensity = 0.6,
      treeDensity = 0.2,
      dirtDensity = 0.15
    )
    
    println("\n=== AI-Readable Configuration Output ===")
    println(s"Bounds: ${bounds.describe}")
    println(s"DungeonConfig: bounds=${dungeonConfig.bounds.map(_.describe).getOrElse("None")}, " +
            s"entranceSide=${dungeonConfig.entranceSide}, size=${dungeonConfig.size}, " +
            s"lockedDoors=${dungeonConfig.lockedDoorCount}, items=${dungeonConfig.itemCount}")
    println(s"Entrance Room: ${dungeonConfig.getEntranceRoom}")
    println(s"WorldConfig: grass=${worldConfig.grassDensity}, tree=${worldConfig.treeDensity}, " +
            s"dirt=${worldConfig.dirtDensity}, walkablePaths=${worldConfig.ensureWalkablePaths}, " +
            s"perimeterTrees=${worldConfig.perimeterTrees}")
    println("=========================================\n")
    
    // Verify output is present
    assert(bounds.describe.nonEmpty)
  }
}
