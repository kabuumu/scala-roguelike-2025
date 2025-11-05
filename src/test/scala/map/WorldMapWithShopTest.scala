package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class WorldMapWithShopTest extends AnyFunSuite {
  
  test("WorldMapGenerator creates shop in world") {
    val config = WorldMapConfig(
      worldConfig = WorldConfig(
        bounds = MapBounds(-5, 5, -5, 5),
        grassDensity = 0.65,
        treeDensity = 0.20,
        dirtDensity = 0.10,
        ensureWalkablePaths = true,
        perimeterTrees = true,
        seed = 12345
      )
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    // Shop should be present
    assert(worldMap.shop.isDefined, "WorldMap should contain a shop")
    
    val shop = worldMap.shop.get
    
    // Shop should have tiles
    assert(shop.tiles.nonEmpty, "Shop should have tiles")
    
    // Shop should have walls
    assert(shop.walls.nonEmpty, "Shop should have walls")
    
    // Shop should have center tile
    val center = shop.centerTile
    assert(shop.tiles.contains(center), "Shop should contain its center tile")
    
    // Shop should have entrance
    val entrance = shop.entranceTile
    assert(shop.tiles.contains(entrance), "Shop should contain its entrance tile")
    assert(shop.tiles(entrance) == TileType.Floor, "Shop entrance should be floor")
    
    println(s"Shop location: ${shop.location}")
    println(s"Shop center: ${shop.centerTile}")
    println(s"Shop entrance: ${shop.entranceTile}")
    println(s"Shop has ${shop.tiles.size} tiles and ${shop.walls.size} walls")
  }
  
  test("Shop is included in world tiles") {
    val config = WorldMapConfig(
      worldConfig = WorldConfig(
        bounds = MapBounds(-5, 5, -5, 5),
        seed = 54321
      ),
      numRivers = 0  // Disable rivers for this test to avoid seed conflicts
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    val shop = worldMap.shop.get
    
    // All shop tiles should be in world tiles
    // Note: Some shop tiles may be overridden by paths (paths are added last)
    shop.tiles.foreach { case (point, tileType) =>
      assert(worldMap.tiles.contains(point), s"Shop tile at $point should be in world tiles")
      // Paths may override shop tiles with Dirt or Bridge (if crossing a river)
      val actualType = worldMap.tiles(point)
      val isValidOverride = actualType == tileType || 
                           (actualType == TileType.Dirt && worldMap.paths.contains(point)) ||
                           (actualType == TileType.Bridge && worldMap.bridges.contains(point))
      assert(isValidOverride, s"Shop tile at $point should be $tileType, Dirt (if on path), or Bridge (if on river crossing), but was $actualType")
    }
  }
  
  test("Path leads to shop entrance") {
    val config = WorldMapConfig(
      worldConfig = WorldConfig(
        bounds = MapBounds(-5, 5, -5, 5),
        seed = 99999
      )
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    val shop = worldMap.shop.get
    val shopEntrance = shop.entranceTile
    
    // Path should include tiles leading toward shop
    // The path uses Bresenham line from origin (0,0) to shop entrance
    val pathExists = worldMap.paths.nonEmpty
    assert(pathExists, "Paths should exist in world")
    
    // Shop entrance or nearby tiles should be part of paths
    val nearShopPath = worldMap.paths.exists { pathPoint =>
      val distance = math.abs(pathPoint.x - shopEntrance.x) + math.abs(pathPoint.y - shopEntrance.y)
      distance <= 5
    }
    assert(nearShopPath, "Path should lead near shop entrance")
    
    println(s"Total path tiles: ${worldMap.paths.size}")
    println(s"Shop entrance at: $shopEntrance")
  }
  
  test("Shop doesn't overlap with dungeon") {
    val config = WorldMapConfig(
      worldConfig = WorldConfig(
        bounds = MapBounds(-5, 5, -5, 5),
        seed = 11111
      )
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    val shop = worldMap.shop.get
    
    // Get dungeon tiles
    val dungeonTiles = worldMap.dungeons.flatMap(_.tiles.keySet).toSet
    val shopTiles = shop.tiles.keySet
    
    // Shop and dungeon should not overlap
    val overlap = shopTiles.intersect(dungeonTiles)
    assert(overlap.isEmpty, s"Shop and dungeon should not overlap, but found ${overlap.size} overlapping tiles")
  }
  
  test("Shop is near origin") {
    val config = WorldMapConfig(
      worldConfig = WorldConfig(
        bounds = MapBounds(-5, 5, -5, 5),
        seed = 22222
      )
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    val shop = worldMap.shop.get
    
    // Shop should be relatively close to origin (within 10 rooms)
    val distanceFromOrigin = math.abs(shop.location.x) + math.abs(shop.location.y)
    assert(distanceFromOrigin <= 10, s"Shop should be near origin, but is at distance $distanceFromOrigin")
    
    println(s"Shop is at ${shop.location}, distance from origin: $distanceFromOrigin rooms")
  }
}
