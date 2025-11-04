package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class MultipleDungeonsTest extends AnyFunSuite {
  
  test("World map of 21x21 (-10 to 10) generates 4 dungeons") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val config = WorldMapConfig(
      worldConfig = WorldConfig(bounds, seed = 12345)
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    println(s"\n=== Multiple Dungeons Test ===")
    println(s"World bounds: ${bounds.describe}")
    println(s"Number of dungeons: ${worldMap.dungeons.size}")
    
    assert(worldMap.dungeons.size == 4, 
      s"Expected 4 dungeons for 21x21 world, got ${worldMap.dungeons.size}")
    
    // Verify each dungeon has reasonable bounds
    worldMap.dungeons.zipWithIndex.foreach { case (dungeon, idx) =>
      val roomGrid = dungeon.roomGrid
      val minX = roomGrid.map(_.x).min
      val maxX = roomGrid.map(_.x).max
      val minY = roomGrid.map(_.y).min
      val maxY = roomGrid.map(_.y).max
      
      val dungeonBounds = MapBounds(minX, maxX, minY, maxY)
      
      println(s"\nDungeon $idx:")
      println(s"  Bounds: ${dungeonBounds.describe}")
      println(s"  Rooms: ${roomGrid.size}")
      println(s"  Start point: ${dungeon.startPoint}")
      
      assert(roomGrid.nonEmpty, s"Dungeon $idx should have rooms")
      // Dungeons should have at least 10 rooms to be meaningful
      assert(roomGrid.size >= 10, 
        s"Dungeon $idx should have at least 10 rooms, got ${roomGrid.size}")
    }
    
    // Verify dungeons don't overlap by checking room positions
    val allRoomPositions = worldMap.dungeons.flatMap(_.roomGrid).toSet
    val totalRooms = worldMap.dungeons.map(_.roomGrid.size).sum
    
    println(s"\nTotal rooms across all dungeons: $totalRooms")
    println(s"Unique room positions: ${allRoomPositions.size}")
    
    // Some overlap is okay since dungeons may share boundary rooms,
    // but they should mostly be distinct
    val overlapPercent = (1.0 - allRoomPositions.size.toDouble / totalRooms) * 100
    println(s"Room overlap: ${overlapPercent.toInt}%")
    
    assert(overlapPercent < 10, 
      s"Dungeons should have minimal overlap, got ${overlapPercent.toInt}%")
    
    println(s"\n✅ CONFIRMED: 4 dungeons generated for 21x21 world with minimal overlap")
  }
  
  test("calculateDungeonConfigs produces correct number of dungeons") {
    // Test various world sizes
    val testCases = Seq(
      (MapBounds(-5, 5, -5, 5), 1),    // 11x11 = 121 rooms² -> 1 dungeon
      (MapBounds(-10, 10, -10, 10), 4), // 21x21 = 441 rooms² -> 4 dungeons
      (MapBounds(-15, 15, -15, 15), 10), // 31x31 = 961 rooms² -> 10 dungeons (rounded)
    )
    
    println("\n=== Dungeon Count Scaling Test ===")
    
    testCases.foreach { case (bounds, expectedCount) =>
      val configs = WorldMapGenerator.calculateDungeonConfigs(bounds, 12345L)
      
      println(s"World ${bounds.roomWidth}x${bounds.roomHeight} (${bounds.roomArea} rooms²):")
      println(s"  Expected: ~$expectedCount dungeons")
      println(s"  Generated: ${configs.size} dungeons")
      
      assert(configs.size == expectedCount, 
        s"Expected $expectedCount dungeons for ${bounds.describe}, got ${configs.size}")
      
      // Verify each config has valid bounds (at least 5x5 to be viable)
      configs.zipWithIndex.foreach { case (config, idx) =>
        assert(config.bounds.roomWidth >= 5, 
          s"Config $idx width should be at least 5, got ${config.bounds.roomWidth}")
        assert(config.bounds.roomHeight >= 5, 
          s"Config $idx height should be at least 5, got ${config.bounds.roomHeight}")
      }
    }
    
    println("\n✅ CONFIRMED: Dungeon count scales correctly with world size")
  }
  
  test("Dungeons are positioned in different regions") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val configs = WorldMapGenerator.calculateDungeonConfigs(bounds, 12345L)
    
    println("\n=== Dungeon Positioning Test ===")
    
    // For a 2x2 grid of dungeons, check they're in different quadrants
    val centerX = (bounds.minRoomX + bounds.maxRoomX) / 2
    val centerY = (bounds.minRoomY + bounds.maxRoomY) / 2
    
    val quadrants = configs.map { config =>
      val configCenterX = (config.bounds.minRoomX + config.bounds.maxRoomX) / 2
      val configCenterY = (config.bounds.minRoomY + config.bounds.maxRoomY) / 2
      
      val quadrant = (configCenterX < centerX, configCenterY < centerY) match {
        case (true, true) => "Top-Left"
        case (true, false) => "Bottom-Left"
        case (false, true) => "Top-Right"
        case (false, false) => "Bottom-Right"
      }
      
      println(s"Dungeon at ${config.bounds.describe}")
      println(s"  Center: ($configCenterX, $configCenterY)")
      println(s"  Quadrant: $quadrant")
      
      quadrant
    }
    
    // Each quadrant should have exactly one dungeon
    val quadrantCounts = quadrants.groupBy(identity).view.mapValues(_.size).toMap
    
    println(s"\nQuadrant distribution: $quadrantCounts")
    
    assert(quadrantCounts.values.forall(_ == 1), 
      "Each quadrant should have exactly one dungeon")
    
    println("\n✅ CONFIRMED: Dungeons are evenly distributed across quadrants")
  }
  
  test("Dungeon entrances face toward player spawn (center)") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val configs = WorldMapGenerator.calculateDungeonConfigs(bounds, 12345L)
    
    println("\n=== Dungeon Entrance Orientation Test ===")
    println("Player spawns at center (0, 0)")
    
    configs.zipWithIndex.foreach { case (config, idx) =>
      val configCenterX = (config.bounds.minRoomX + config.bounds.maxRoomX) / 2
      val configCenterY = (config.bounds.minRoomY + config.bounds.maxRoomY) / 2
      
      // Determine quadrant
      val quadrant = if (configCenterX < 0 && configCenterY < 0) "Top-Left"
                     else if (configCenterX >= 0 && configCenterY < 0) "Top-Right"
                     else if (configCenterX < 0 && configCenterY >= 0) "Bottom-Left"
                     else "Bottom-Right"
      
      println(s"\nDungeon $idx in $quadrant quadrant:")
      println(s"  Center: ($configCenterX, $configCenterY)")
      println(s"  Entrance faces: ${config.entranceSide}")
      
      // Verify entrance faces toward center
      val expectedDirection = if (configCenterX < 0) {
        // Left side of center: should face Right (toward center)
        game.Direction.Right
      } else {
        // Right side of center: should face Left (toward center)
        game.Direction.Left
      }
      
      assert(config.entranceSide == expectedDirection,
        s"Dungeon $idx in $quadrant should face $expectedDirection (toward center), but faces ${config.entranceSide}")
    }
    
    println("\n✅ CONFIRMED: All dungeon entrances face toward player spawn area")
  }
}
