package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class WorldMutatorTest extends AnyFunSuite {
  
  test("TerrainMutator adds terrain tiles to empty world") {
    val bounds = MapBounds(0, 2, 0, 2)
    val config = WorldConfig(bounds, seed = 12345)
    val mutator = new TerrainMutator(config)
    
    val emptyWorld = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val mutatedWorld = mutator.mutateWorld(emptyWorld)
    
    assert(mutatedWorld.tiles.nonEmpty, "TerrainMutator should add terrain tiles")
    
    // Check that terrain tiles are within expected bounds
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    val expectedTiles = (tileMaxX - tileMinX + 1) * (tileMaxY - tileMinY + 1)
    assert(mutatedWorld.tiles.size >= expectedTiles, 
           s"Expected at least $expectedTiles tiles, got ${mutatedWorld.tiles.size}")
    
    println(s"TerrainMutator added ${mutatedWorld.tiles.size} tiles")
  }
  
  test("DungeonPlacementMutator adds dungeon to world") {
    val bounds = MapBounds(-5, 5, -5, 5)
    val dungeonConfig = DungeonConfig(bounds, seed = 54321)
    val mutator = new DungeonPlacementMutator(Seq(dungeonConfig))
    
    val worldWithTerrain = WorldMap(
      tiles = Map(Point(0, 0) -> TileType.Grass1),
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val mutatedWorld = mutator.mutateWorld(worldWithTerrain)
    
    assert(mutatedWorld.dungeons.size == 1, "Should have one dungeon")
    assert(mutatedWorld.dungeons.head.roomGrid.nonEmpty, "Dungeon should have rooms")
    assert(mutatedWorld.tiles.size > 1, "Should have dungeon tiles added")
    
    println(s"DungeonPlacementMutator added dungeon with ${mutatedWorld.dungeons.head.roomGrid.size} rooms")
  }
  
  test("ShopPlacementMutator adds shop to world") {
    val bounds = MapBounds(-5, 5, -5, 5)
    val mutator = new ShopPlacementMutator(bounds)
    
    val dungeonConfig = DungeonConfig(MapBounds(1, 5, -5, 0), seed = 111)
    val dungeon = DungeonGenerator.generateDungeon(dungeonConfig)
    
    val worldWithDungeon = WorldMap(
      tiles = Map(Point(0, 0) -> TileType.Grass1),
      dungeons = Seq(dungeon),
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val mutatedWorld = mutator.mutateWorld(worldWithDungeon)
    
    assert(mutatedWorld.shop.isDefined, "Should have a shop")
    assert(mutatedWorld.shop.get.tiles.nonEmpty, "Shop should have tiles")
    
    println(s"ShopPlacementMutator added shop at ${mutatedWorld.shop.get.location}")
  }
  
  test("PathGenerationMutator creates paths to destinations") {
    val startPoint = Point(0, 0)
    val mutator = new PathGenerationMutator(startPoint)
    
    val dungeonConfig = DungeonConfig(MapBounds(1, 5, -5, 0), seed = 222)
    val dungeon = DungeonGenerator.generateDungeon(dungeonConfig)
    
    val shop = Shop(Point(10, 10))
    
    val worldWithFeatures = WorldMap(
      tiles = Map(Point(0, 0) -> TileType.Grass1),
      dungeons = Seq(dungeon),
      shop = Some(shop),
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = MapBounds(-10, 10, -10, 10)
    )
    
    val mutatedWorld = mutator.mutateWorld(worldWithFeatures)
    
    assert(mutatedWorld.paths.nonEmpty, "Should have created paths")
    assert(mutatedWorld.paths.contains(startPoint), "Path should include start point")
    
    println(s"PathGenerationMutator created ${mutatedWorld.paths.size} path tiles")
  }
  
  test("Multiple mutators can be chained together") {
    val bounds = MapBounds(-3, 3, -3, 3)
    val config = WorldConfig(bounds, seed = 99999)
    val startPoint = Point(0, 0)
    
    val dungeonConfig = DungeonConfig(MapBounds(1, 3, -3, 0), seed = 333)
    
    val mutators: Seq[WorldMutator] = Seq(
      new TerrainMutator(config),
      new DungeonPlacementMutator(Seq(dungeonConfig)),
      new ShopPlacementMutator(bounds),
      new PathGenerationMutator(startPoint)
    )
    
    val initialWorld = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val finalWorld = mutators.foldLeft(initialWorld) { (world, mutator) =>
      mutator.mutateWorld(world)
    }
    
    assert(finalWorld.tiles.nonEmpty, "Should have terrain tiles")
    assert(finalWorld.dungeons.nonEmpty, "Should have dungeons")
    assert(finalWorld.shop.isDefined, "Should have a shop")
    assert(finalWorld.paths.nonEmpty, "Should have paths")
    
    println("Successfully chained multiple mutators:")
    println(s"  - Terrain tiles: ${finalWorld.tiles.size}")
    println(s"  - Dungeons: ${finalWorld.dungeons.size}")
    println(s"  - Shop: ${finalWorld.shop.isDefined}")
    println(s"  - Path tiles: ${finalWorld.paths.size}")
  }
  
  test("WorldMapGenerator.generateWorldMapWithMutators allows custom mutator list") {
    val bounds = MapBounds(-2, 2, -2, 2)
    val config = WorldConfig(bounds, seed = 77777)
    
    // Create only terrain, no dungeons or other features
    val customMutators: Seq[WorldMutator] = Seq(
      new TerrainMutator(config)
    )
    
    val initialWorld = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val world = WorldMapGenerator.generateWorldMapWithMutators(initialWorld, customMutators)
    
    assert(world.tiles.nonEmpty, "Should have terrain")
    assert(world.dungeons.isEmpty, "Should have no dungeons")
    assert(world.shop.isEmpty, "Should have no shop")
    assert(world.paths.isEmpty, "Should have no paths")
    
    println("Custom mutator list successfully applied (terrain only)")
  }
  
  test("WalkablePathsMutator preserves world when ensureWalkablePaths is false") {
    val bounds = MapBounds(0, 2, 0, 2)
    val config = WorldConfig(bounds, ensureWalkablePaths = false, seed = 44444)
    val mutator = new WalkablePathsMutator(config)
    
    val world = WorldMap(
      tiles = Map(Point(0, 0) -> TileType.Tree),
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val mutatedWorld = mutator.mutateWorld(world)
    
    // Should not modify the world since ensureWalkablePaths is false
    assert(mutatedWorld.tiles == world.tiles, "WalkablePathsMutator should not modify when disabled")
    
    println("WalkablePathsMutator correctly respects configuration")
  }
  
  test("Mutators are extensible - custom mutator can be added") {
    // Define a custom mutator that adds rocks around the perimeter
    class PerimeterRockMutator(bounds: MapBounds) extends WorldMutator {
      override def mutateWorld(worldMap: WorldMap): WorldMap = {
        val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
        
        val perimeterRocks = (for {
          x <- tileMinX to tileMaxX
          y <- tileMinY to tileMaxY
          if x == tileMinX || x == tileMaxX || y == tileMinY || y == tileMaxY
        } yield Point(x, y) -> TileType.Rock).toMap
        
        worldMap.copy(tiles = worldMap.tiles ++ perimeterRocks)
      }
    }
    
    val bounds = MapBounds(0, 1, 0, 1)
    val customMutator = new PerimeterRockMutator(bounds)
    
    val world = WorldMap(
      tiles = Map(Point(5, 5) -> TileType.Grass1),
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val mutatedWorld = customMutator.mutateWorld(world)
    
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    val expectedRocks = 2 * (tileMaxX - tileMinX + 1) + 2 * (tileMaxY - tileMinY + 1) - 4
    val actualRocks = mutatedWorld.tiles.values.count(_ == TileType.Rock)
    
    assert(actualRocks >= expectedRocks, s"Should have at least $expectedRocks perimeter rocks")
    
    println(s"Custom PerimeterRockMutator successfully added $actualRocks rocks")
  }
  
  test("WorldMapGenerator uses mutators for generation") {
    val bounds = MapBounds(-5, 5, -5, 5)
    val config = WorldMapConfig(
      worldConfig = WorldConfig(bounds, seed = 12345)
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    // Verify all expected features are present
    assert(worldMap.tiles.nonEmpty, "World should have tiles")
    assert(worldMap.dungeons.nonEmpty, "World should have dungeons")
    assert(worldMap.shop.isDefined, "World should have a shop")
    assert(worldMap.paths.nonEmpty, "World should have paths")
    
    println("WorldMapGenerator successfully uses mutator pattern:")
    println(s"  - Total tiles: ${worldMap.tiles.size}")
    println(s"  - Dungeons: ${worldMap.dungeons.size}")
    println(s"  - Shop present: ${worldMap.shop.isDefined}")
    println(s"  - Path tiles: ${worldMap.paths.size}")
  }
}
