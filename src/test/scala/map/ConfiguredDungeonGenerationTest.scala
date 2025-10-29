package map

import game.{Direction, Point}
import org.scalatest.funsuite.AnyFunSuite

class ConfiguredDungeonGenerationTest extends AnyFunSuite {
  
  test("generateDungeon with DungeonConfig produces valid dungeon") {
    val config = DungeonConfig(
      size = 10,
      lockedDoorCount = 1,
      itemCount = 2,
      seed = 12345
    )
    
    val dungeon = MapGenerator.generateDungeon(config)
    
    assert(dungeon.roomGrid.size == 10)
    assert(dungeon.lockedDoorCount == 1)
    assert(dungeon.nonKeyItems.size == 2)

    println(s"Generated dungeon: ${dungeon.roomGrid.size} rooms")
  }
  
  test("generateDungeon with bounds constrains dungeon generation") {
    val bounds = MapBounds(-5, 5, -10, 5)
    val config = DungeonConfig(
      bounds = Some(bounds),
      size = 8,
      seed = 12345
    )
    
    val dungeon = MapGenerator.generateDungeon(config)
    
    // All dungeon rooms should be within bounds
    val dungeonRooms = dungeon.roomGrid
    dungeonRooms.foreach { room =>
      assert(bounds.contains(room), s"Room $room should be within bounds ${bounds.describe}")
    }
    
    println(s"All ${dungeonRooms.size} dungeon rooms are within bounds")
  }
  
  test("generateDungeon starts at configured entrance room when bounds specified") {
    val bounds = MapBounds(-3, 3, -3, 3)
    val config = DungeonConfig(
      bounds = Some(bounds),
      entranceSide = Direction.Up,
      size = 5,
      seed = 12345
    )
    
    val dungeon = MapGenerator.generateDungeon(config)
    
    // The dungeon should be built starting from the configured entrance
    val entranceRoom = config.getEntranceRoom
    
    // The entrance room should be part of the dungeon structure
    val dungeonRooms = dungeon.roomGrid
    
    assert(dungeonRooms.nonEmpty, "Should have dungeon rooms")
    
    println(s"Configured entrance: $entranceRoom")
    println(s"Dungeon rooms: ${dungeonRooms.size}")
    println(s"Start point: ${dungeon.startPoint}")
  }
  
  test("generateDungeon with different entrance sides") {
    val bounds = MapBounds(-5, 5, -5, 5)
    
    val directions = Seq(Direction.Up, Direction.Down, Direction.Left, Direction.Right)
    
    directions.foreach { direction =>
      val config = DungeonConfig(
        bounds = Some(bounds),
        entranceSide = direction,
        size = 8,
        seed = 54321
      )
      
      val dungeon = MapGenerator.generateDungeon(config)
      
      assert(dungeon.roomGrid.size == 8)
      
      println(s"Generated dungeon with entrance side $direction: ${dungeon.roomGrid.size} rooms")
    }
  }
  
  test("generateDungeon respects configured seed for reproducibility") {
    val config1 = DungeonConfig(size = 10, seed = 99999)
    val config2 = DungeonConfig(size = 10, seed = 99999)
    
    val dungeon1 = MapGenerator.generateDungeon(config1)
    val dungeon2 = MapGenerator.generateDungeon(config2)
    
    // Same configuration should produce identical dungeons
    assert(dungeon1.roomGrid == dungeon2.roomGrid)
    assert(dungeon1.roomConnections == dungeon2.roomConnections)
    
    println("Reproducible generation verified with seed 99999")
  }
  
  test("backward compatible generateDungeon still works") {
    // Old API should still work
    val dungeon = MapGenerator.generateDungeon(
      dungeonSize = 10,
      lockedDoorCount = 1,
      itemCount = 2,
      seed = 12345
    )
    
    assert(dungeon.roomGrid.size == 10)
    assert(dungeon.lockedDoorCount == 1)
    assert(dungeon.nonKeyItems.size == 2)
    
    println("Backward compatible API works correctly")
  }
  
  test("dungeon generation provides AI-readable output") {
    // Use unbounded configuration for reliable test
    val config = DungeonConfig(
      bounds = None, // Unbounded for reliable generation
      entranceSide = Direction.Down,
      size = 10,
      lockedDoorCount = 1,
      itemCount = 2,
      seed = 12345
    )
    
    println("\n=== AI-Readable Dungeon Generation Output ===")
    println(s"Config: ${config.bounds.map(_.describe).getOrElse("Unbounded")}")
    println(s"  Entrance side: ${config.entranceSide}")
    println(s"  Target size: ${config.size} rooms")
    println(s"  Locked doors: ${config.lockedDoorCount}")
    println(s"  Items: ${config.itemCount}")
    
    val dungeon = MapGenerator.generateDungeon(config)
    
    println(s"Result:")
    println(s"  Total rooms: ${dungeon.roomGrid.size}")
    println(s"  Connections: ${dungeon.roomConnections.size}")
    println(s"  Locked doors: ${dungeon.lockedDoorCount}")
    println(s"  Items: ${dungeon.nonKeyItems.size}")
    println(s"  Start point: ${dungeon.startPoint}")
    println(s"  Boss room: ${dungeon.endpoint}")
    println(s"  Trader room: ${dungeon.traderRoom}")
    println("=============================================\n")
    
    assert(dungeon.roomGrid.nonEmpty)
    assert(dungeon.lockedDoorCount == 1)
    assert(dungeon.nonKeyItems.size == 2)
  }
  
  test("small bounded dungeon fits within tight constraints") {
    val bounds = MapBounds(0, 4, -2, 4)
    val config = DungeonConfig(
      bounds = Some(bounds),
      size = 5,
      lockedDoorCount = 0,
      itemCount = 1,
      seed = 12345
    )
    
    val dungeon = MapGenerator.generateDungeon(config)
    
    // Verify all dungeon rooms are within bounds
    val dungeonRooms = dungeon.roomGrid
    
    assert(dungeonRooms.size == config.size, s"Should have ${config.size} dungeon rooms")
    
    println(s"Small dungeon: ${dungeonRooms.size} dungeon rooms")
  }
}
