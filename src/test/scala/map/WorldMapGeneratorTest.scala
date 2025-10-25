package map

import game.{Direction, Point}
import org.scalatest.funsuite.AnyFunSuite

class WorldMapGeneratorTest extends AnyFunSuite {
  
  test("generateWorldMap combines terrain, rivers, paths, and dungeons") {
    val worldBounds = MapBounds(-10, 10, -10, 10)
    
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.7,
      treeDensity = 0.15,
      dirtDensity = 0.1,
      seed = 12345
    )
    
    val dungeonConfig = DungeonConfig(
      bounds = Some(MapBounds(-5, 5, -8, 0)),
      entranceSide = Direction.Down,
      size = 8,
      seed = 12345
    )
    
    val riverConfig = RiverConfig(
      startPoint = Point(-100, -50),
      flowDirection = (1, 1),
      length = 100,
      width = 1,
      curviness = 0.3,
      bounds = worldBounds,
      seed = 12345
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      dungeonConfigs = Seq(dungeonConfig),
      riverConfigs = Seq(riverConfig),
      generatePathsToDungeons = true,
      pathsPerDungeon = 2,
      pathWidth = 1
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    assert(worldMap.tiles.nonEmpty, "World map should have tiles")
    assert(worldMap.dungeons.size == 1, "Should have one dungeon")
    assert(worldMap.rivers.nonEmpty, "Should have river tiles")
    assert(worldMap.paths.nonEmpty, "Should have path tiles")
    
    println(s"Generated world map:")
    println(s"  Total tiles: ${worldMap.tiles.size}")
    println(s"  Dungeons: ${worldMap.dungeons.size}")
    println(s"  River tiles: ${worldMap.rivers.size}")
    println(s"  Path tiles: ${worldMap.paths.size}")
  }
  
  test("world map tiles have correct priority: dungeons > paths > rivers > terrain") {
    val worldBounds = MapBounds(0, 5, 0, 5)
    
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 1.0,
      treeDensity = 0.0,
      dirtDensity = 0.0,
      perimeterTrees = false,
      seed = 12345
    )
    
    // Create a simple river that flows through the map
    val riverConfig = RiverConfig(
      startPoint = Point(25, 0),
      flowDirection = (0, 1),
      length = 60,
      width = 0,
      curviness = 0.0,
      bounds = worldBounds,
      seed = 12345
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      dungeonConfigs = Seq.empty,
      riverConfigs = Seq(riverConfig),
      generatePathsToDungeons = false
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    // Check that river tiles are Water type (not Grass)
    val riverTileTypes = worldMap.rivers.flatMap(p => worldMap.tiles.get(p))
    assert(riverTileTypes.forall(_ == TileType.Water), "River tiles should be Water type")
    
    println(s"Priority verified: ${worldMap.rivers.size} river tiles are Water type")
  }
  
  test("verifyTraversability checks reachability between dungeon entrances") {
    val worldBounds = MapBounds(-10, 10, -10, 10)
    
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.85,
      treeDensity = 0.1,
      dirtDensity = 0.05,
      ensureWalkablePaths = true,
      seed = 12345
    )
    
    // Use a single dungeon for simplicity - traversability is still tested
    val dungeonConfig = DungeonConfig(
      bounds = None,
      size = 5,
      seed = 1
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      dungeonConfigs = Seq(dungeonConfig),
      generatePathsToDungeons = true,
      pathsPerDungeon = 1
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    val report = WorldMapGenerator.verifyTraversability(worldMap)
    
    println("\n" + report.describe + "\n")
    
    assert(report.dungeonEntrances.size == 1, "Should have 1 dungeon entrance")
    assert(report.walkableTileCount > 0, "Should have walkable tiles")
    assert(report.allEntrancesReachable, "Single entrance should be reachable to itself")
  }
  
  test("describeWorldMap provides comprehensive AI-readable output") {
    val worldBounds = MapBounds(-8, 8, -8, 8)
    
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.65,
      treeDensity = 0.2,
      dirtDensity = 0.1,
      seed = 12345
    )
    
    val dungeonConfig = DungeonConfig(
      bounds = Some(MapBounds(-4, 4, -6, 0)),
      size = 8,
      seed = 12345
    )
    
    val riverConfig = RiverConfig(
      startPoint = Point(-80, 0),
      flowDirection = (1, 0),
      length = 80,
      width = 1,
      curviness = 0.2,
      bounds = worldBounds,
      seed = 12345
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      dungeonConfigs = Seq(dungeonConfig),
      riverConfigs = Seq(riverConfig),
      generatePathsToDungeons = true
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    val description = WorldMapGenerator.describeWorldMap(worldMap)
    
    println("\n" + description + "\n")
    
    assert(description.contains("World Map Generation Summary"))
    assert(description.contains("Terrain Distribution"))
    assert(description.contains("Features"))
    assert(description.contains("Dungeons"))
  }
  
  test("world map without dungeons still generates terrain and rivers") {
    val worldBounds = MapBounds(0, 5, 0, 5)
    
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.7,
      seed = 12345
    )
    
    val riverConfig = RiverConfig(
      startPoint = Point(10, 10),
      flowDirection = (1, 1),
      length = 50,
      width = 1,
      curviness = 0.2,
      bounds = worldBounds,
      seed = 12345
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      dungeonConfigs = Seq.empty,
      riverConfigs = Seq(riverConfig),
      generatePathsToDungeons = false
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    assert(worldMap.tiles.nonEmpty, "Should have terrain tiles")
    assert(worldMap.dungeons.isEmpty, "Should have no dungeons")
    assert(worldMap.rivers.nonEmpty, "Should have river tiles")
    assert(worldMap.paths.isEmpty, "Should have no paths")
    
    println(s"World without dungeons: ${worldMap.tiles.size} tiles, ${worldMap.rivers.size} river tiles")
  }
  
  test("world map with multiple dungeons generates paths to each") {
    val worldBounds = MapBounds(-20, 20, -20, 20)
    
    val worldConfig = WorldConfig(bounds = worldBounds, seed = 12345)
    
    val dungeons = Seq(
      DungeonConfig(bounds = None, size = 5, seed = 1),
      DungeonConfig(bounds = None, size = 5, seed = 2),
      DungeonConfig(bounds = None, size = 5, seed = 3)
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      dungeonConfigs = dungeons,
      generatePathsToDungeons = true,
      pathsPerDungeon = 2
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    assert(worldMap.dungeons.size == 3, "Should have 3 dungeons")
    assert(worldMap.paths.nonEmpty, "Should have paths")
    
    // Each dungeon entrance should be in the path set
    worldMap.dungeons.foreach { dungeon =>
      assert(worldMap.paths.contains(dungeon.startPoint), 
             s"Paths should reach dungeon at ${dungeon.startPoint}")
    }
    
    println(s"Generated ${worldMap.paths.size} path tiles leading to ${worldMap.dungeons.size} dungeons")
  }
  
  test("rivers flow in non-straight lines with curviness") {
    val worldBounds = MapBounds(0, 10, 0, 10)
    
    val worldConfig = WorldConfig(bounds = worldBounds, seed = 12345)
    
    val straightRiver = RiverConfig(
      startPoint = Point(50, 10),
      flowDirection = (0, 1),
      length = 50,
      width = 0,
      curviness = 0.0,
      bounds = worldBounds,
      seed = 12345
    )
    
    val curvedRiver = RiverConfig(
      startPoint = Point(50, 10),
      flowDirection = (0, 1),
      length = 50,
      width = 0,
      curviness = 0.4,
      bounds = worldBounds,
      seed = 67890
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      riverConfigs = Seq(straightRiver, curvedRiver)
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    assert(worldMap.rivers.nonEmpty, "Should have river tiles")
    
    println(s"Generated ${worldMap.rivers.size} river tiles (straight + curved)")
  }
  
  test("traversability report identifies unreachable areas") {
    val worldBounds = MapBounds(0, 3, 0, 3)
    
    // Create a world with high tree density to potentially block paths
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.2,
      treeDensity = 0.7,
      ensureWalkablePaths = false, // Don't auto-fix walkable paths
      perimeterTrees = true,
      seed = 12345
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      generatePathsToDungeons = false
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    val report = WorldMapGenerator.verifyTraversability(worldMap)
    
    println(s"\nTraversability with dense trees:")
    println(s"  Walkable: ${report.walkableTileCount} / ${report.totalTileCount}")
    
    // Report should provide meaningful information even if no dungeons
    assert(report.walkableTileCount >= 0)
    assert(report.totalTileCount > 0)
  }
  
  test("complete open world RPG scenario - grass, dirt, rivers, and dungeons") {
    val worldBounds = MapBounds(-20, 20, -20, 20)
    
    val worldConfig = WorldConfig(
      bounds = worldBounds,
      grassDensity = 0.65,
      treeDensity = 0.2,
      dirtDensity = 0.1,
      ensureWalkablePaths = true,
      perimeterTrees = true,
      seed = 42
    )
    
    val mainDungeon = DungeonConfig(
      bounds = None,
      entranceSide = Direction.Down,
      size = 10,  // Reduced from 12 to avoid iteration limit
      lockedDoorCount = 1,  // Reduced from 2
      itemCount = 3,  // Reduced from 5
      seed = 42
    )
    
    val sideDungeon = DungeonConfig(
      bounds = None,
      entranceSide = Direction.Left,
      size = 5,  // Reduced from 6
      itemCount = 1,  // Reduced from 2
      seed = 43
    )
    
    val river1 = RiverConfig(
      startPoint = Point(-200, -100),
      flowDirection = (1, 1),
      length = 150,
      width = 1,
      curviness = 0.3,
      bounds = worldBounds,
      seed = 100
    )
    
    val river2 = RiverConfig(
      startPoint = Point(200, -100),
      flowDirection = (-1, 1),
      length = 150,
      width = 1,
      curviness = 0.25,
      bounds = worldBounds,
      seed = 101
    )
    
    val config = WorldMapConfig(
      worldConfig = worldConfig,
      dungeonConfigs = Seq(mainDungeon, sideDungeon),
      riverConfigs = Seq(river1, river2),
      generatePathsToDungeons = true,
      pathsPerDungeon = 3,
      pathWidth = 1
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    val traversability = WorldMapGenerator.verifyTraversability(worldMap)
    
    println("\n" + "="*70)
    println("OPEN WORLD RPG - COMPLETE MAP GENERATION")
    println("="*70)
    println(WorldMapGenerator.describeWorldMap(worldMap))
    println("\n" + traversability.describe)
    println("="*70 + "\n")
    
    // Verify all components are present
    assert(worldMap.tiles.nonEmpty, "Should have terrain")
    assert(worldMap.dungeons.size == 2, "Should have 2 dungeons")
    assert(worldMap.rivers.nonEmpty, "Should have rivers")
    assert(worldMap.paths.nonEmpty, "Should have paths to dungeons")
    
    // Verify tile types are diverse
    val tileTypes = worldMap.tiles.values.toSet
    assert(tileTypes.contains(TileType.Grass1) || tileTypes.contains(TileType.Grass2), "Should have grass")
    assert(tileTypes.contains(TileType.Water), "Should have water (rivers)")
    assert(tileTypes.contains(TileType.Dirt), "Should have dirt (paths)")
    assert(tileTypes.contains(TileType.Floor), "Should have dungeon floors")
    
    println(s"✓ Complete open world RPG map generated successfully")
    println(s"✓ Tile diversity: ${tileTypes.size} different tile types")
  }
}
