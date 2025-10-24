package map

import game.{Direction, Point}
import org.scalatest.funsuite.AnyFunSuite

class WorldWithDungeonIntegrationTest extends AnyFunSuite {
  
  /** Expected number of outdoor transition rooms added to dungeons */
  private val OutdoorRoomCount = 6
  
  test("create world with dungeon in center - basic integration") {
    // Define world bounds (large enough to contain dungeon + surrounding world)
    val worldBounds = MapBounds(-10, 10, -10, 10)
    
    // Create world with natural terrain
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.65,
      treeDensity = 0.2,
      dirtDensity = 0.1,
      ensureWalkablePaths = true,
      perimeterTrees = true,
      seed = 42
    )
    
    val worldTiles = WorldGenerator.generateWorld(worldConfig)
    
    // Create dungeon in the center portion of the world
    val dungeonBounds = MapBounds(-5, 5, -8, 0) // North-central area
    val dungeonConfig = DungeonConfig(
      bounds = Some(dungeonBounds),
      entranceSide = Direction.Down, // Entrance on south side
      size = 10,
      lockedDoorCount = 1,
      itemCount = 3,
      seed = 42
    )
    
    val dungeon = MapGenerator.generateDungeon(dungeonConfig)
    
    // Verify world was created
    assert(worldTiles.nonEmpty, "World should have tiles")
    
    // Verify dungeon was created
    assert(dungeon.roomGrid.nonEmpty, "Dungeon should have rooms")
    assert(dungeon.roomGrid.size == 10 + OutdoorRoomCount, "Total rooms should be dungeon size + outdoor rooms")
    
    // Output AI-readable summary
    println("\n=== World with Dungeon Integration ===")
    println(WorldGenerator.describeWorld(worldTiles, worldConfig))
    println(s"\nDungeon Summary:")
    println(s"  Location: ${dungeonBounds.describe}")
    println(s"  Entrance side: ${dungeonConfig.entranceSide}")
    println(s"  Total rooms: ${dungeon.roomGrid.size}")
    println(s"  Dungeon rooms: ${dungeon.roomGrid.size - dungeon.outdoorRooms.size}")
    println(s"  Outdoor transition rooms: ${dungeon.outdoorRooms.size}")
    println(s"  Start point (outdoor): ${dungeon.startPoint}")
    println(s"  Boss room: ${dungeon.endpoint}")
    println("======================================\n")
  }
  
  test("dungeon bounds demonstration - note: strict bounds may fail") {
    // The dungeon generation algorithm works best with generous bounds
    // Tight bounds can make it impossible to satisfy all constraints
    // (size, trader room, boss room, locked doors, items)
    
    val worldBounds = MapBounds(-15, 15, -15, 15)
    val dungeonBounds = MapBounds(-8, 8, -10, 2) // Centered placement with room to grow
    
    val worldConfig = WorldConfig(bounds = worldBounds, seed = 123)
    val worldTiles = WorldGenerator.generateWorld(worldConfig)
    
    val dungeonConfig = DungeonConfig(
      bounds = Some(dungeonBounds),
      entranceSide = Direction.Down,
      size = 8,
      seed = 123
    )
    
    val dungeon = MapGenerator.generateDungeon(dungeonConfig)
    
    assert(worldTiles.nonEmpty)
    assert(dungeon.roomGrid.size == 8 + OutdoorRoomCount, "Total rooms should be dungeon size + outdoor rooms")
    
    println(s"Bounded dungeon: ${dungeonBounds.describe}, entrance: ${dungeonConfig.entranceSide}")
    println(s"  Generated ${dungeon.roomGrid.size} total rooms")
  }
  
  test("world tiles can be combined with dungeon tiles") {
    val worldBounds = MapBounds(-6, 6, -6, 6)
    val dungeonBounds = MapBounds(-3, 3, -5, 0)
    
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.7,
      treeDensity = 0.15,
      seed = 999
    )
    
    val dungeonConfig = DungeonConfig(
      bounds = Some(dungeonBounds),
      size = 8,
      seed = 999
    )
    
    val worldTiles = WorldGenerator.generateWorld(worldConfig)
    val dungeon = MapGenerator.generateDungeon(dungeonConfig)
    
    // In a real integration, dungeon tiles would override world tiles
    // Here we just verify both can coexist
    val dungeonTileArea = dungeon.tiles.keySet
    val worldTileArea = worldTiles.keySet
    
    // There will be overlap where dungeon overrides world
    val overlap = dungeonTileArea.intersect(worldTileArea)
    
    println(s"\nTile Integration:")
    println(s"  World tiles: ${worldTiles.size}")
    println(s"  Dungeon tiles: ${dungeon.tiles.size}")
    println(s"  Overlap (dungeon overrides world): ${overlap.size}")
    
    // Combined map would use dungeon tiles where they exist, world tiles elsewhere
    val combinedTileCount = worldTiles.size + dungeon.tiles.size - overlap.size
    println(s"  Combined unique tiles: $combinedTileCount")
    
    assert(overlap.nonEmpty, "Dungeon and world should overlap in some areas")
  }
  
  test("AI-readable output for complete world with dungeon") {
    val worldBounds = MapBounds(-12, 12, -12, 12)
    val dungeonBounds = MapBounds(-7, 7, -10, 0) // Generous central-north placement
    
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.6,
      treeDensity = 0.25,
      dirtDensity = 0.1,
      ensureWalkablePaths = true,
      perimeterTrees = true,
      seed = 777
    )
    
    val dungeonConfig = DungeonConfig(
      bounds = Some(dungeonBounds),
      entranceSide = Direction.Down,
      size = 8,  // Modest size for bounded generation
      lockedDoorCount = 1,
      itemCount = 2,
      seed = 777
    )
    
    val worldTiles = WorldGenerator.generateWorld(worldConfig)
    val dungeon = MapGenerator.generateDungeon(dungeonConfig)
    
    println("\n" + "="*60)
    println("COMPLETE WORLD WITH DUNGEON - AI-READABLE OUTPUT")
    println("="*60)
    
    println("\n[WORLD TERRAIN]")
    println(WorldGenerator.describeWorld(worldTiles, worldConfig))
    
    println("\n[DUNGEON STRUCTURE]")
    println(s"Location: ${dungeonBounds.describe}")
    println(s"Entrance side: ${dungeonConfig.entranceSide}")
    println(s"Configuration:")
    println(s"  - Target size: ${dungeonConfig.size} rooms")
    println(s"  - Locked doors: ${dungeonConfig.lockedDoorCount}")
    println(s"  - Items: ${dungeonConfig.itemCount}")
    println(s"Generated dungeon:")
    println(s"  - Total rooms: ${dungeon.roomGrid.size}")
    println(s"  - Dungeon rooms: ${dungeon.roomGrid.size - dungeon.outdoorRooms.size}")
    println(s"  - Outdoor rooms: ${dungeon.outdoorRooms.size}")
    println(s"  - Room connections: ${dungeon.roomConnections.size}")
    println(s"  - Actual locked doors: ${dungeon.lockedDoorCount}")
    println(s"  - Actual items: ${dungeon.nonKeyItems.size}")
    
    println("\n[INTEGRATION POINTS]")
    println(s"  - Player start: ${dungeon.startPoint} (outdoor area)")
    println(s"  - Boss location: ${dungeon.endpoint}")
    println(s"  - Trader location: ${dungeon.traderRoom}")
    
    println("\n[TILE STATISTICS]")
    val dungeonTileCount = dungeon.tiles.size
    val worldTileCount = worldTiles.size
    val overlap = dungeon.tiles.keySet.intersect(worldTiles.keySet).size
    val totalUnique = worldTileCount + dungeonTileCount - overlap
    println(s"  - World tiles: $worldTileCount")
    println(s"  - Dungeon tiles: $dungeonTileCount")
    println(s"  - Overlap: $overlap")
    println(s"  - Total unique tiles: $totalUnique")
    
    println("\n" + "="*60 + "\n")
    
    // Verify everything was created correctly
    assert(worldTiles.nonEmpty)
    assert(dungeon.roomGrid.size == 8 + OutdoorRoomCount, "Total rooms should be dungeon size + outdoor rooms")
    assert(dungeon.lockedDoorCount == 1)
    assert(dungeon.nonKeyItems.size == 2)
  }
  
  test("extensible design allows future map compositions") {
    // This test demonstrates the extensibility of the new design
    // Note: Bounded dungeons work best with generous room to grow
    
    println("\nExtensibility demonstration:")
    println("  The new design supports:")
    println("  - Configurable dungeon bounds (with generous space)")
    println("  - Entrance side configuration (Up/Down/Left/Right)")
    println("  - World terrain generation with natural patterns")
    println("  - Combining multiple map elements")
    println("  Future enhancements could:")
    println("  - Place multiple dungeons in one world")
    println("  - Mix dungeon and world tiles seamlessly")
    println("  - Support more sophisticated layout algorithms")
    
    // Demonstrate with a simple unbounded case
    val simpleConfig = DungeonConfig(size = 5, seed = 999)
    val simpleDungeon = MapGenerator.generateDungeon(simpleConfig)
    
    assert(simpleDungeon.roomGrid.size == 5 + OutdoorRoomCount, "Total rooms should be dungeon size + outdoor rooms")
    println(s"  Example: Simple dungeon with ${simpleDungeon.roomGrid.size} rooms created")
  }
}
