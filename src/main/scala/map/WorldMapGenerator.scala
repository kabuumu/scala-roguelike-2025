package map

import game.Point

/**
 * Unified world map that combines terrain, rivers, paths, and dungeons.
 * This creates an open-world RPG style map with natural flow.
 */
object WorldMapGenerator {
  
  /**
   * Generates a complete world map with terrain, rivers, paths, and dungeons.
   * 
   * @param config WorldMapConfig specifying all map generation parameters
   * @return WorldMap containing all generated elements
   */
  def generateWorldMap(config: WorldMapConfig): WorldMap = {
    // Generate base terrain (grass, dirt, trees)
    val terrainTiles = WorldGenerator.generateWorld(config.worldConfig)
    
    // Generate rivers
    val riverTiles = if (config.riverConfigs.nonEmpty) {
      RiverGenerator.generateRivers(config.riverConfigs)
    } else {
      Set.empty[Point]
    }
    
    // Generate dungeons
    val dungeons = config.dungeonConfigs.map { dungeonConfig =>
      MapGenerator.generateDungeon(dungeonConfig)
    }
    
    // Find dungeon entrance points (outdoor starting rooms)
    val dungeonEntrances = dungeons.map(_.startPoint)
    
    // Generate paths leading to dungeons
    val pathTiles = if (dungeonEntrances.nonEmpty && config.generatePathsToDungeons) {
      PathGenerator.generateDungeonPaths(
        dungeonEntrances.toSeq,
        config.worldConfig.bounds,
        pathsPerEntrance = config.pathsPerDungeon,
        width = config.pathWidth,
        seed = config.worldConfig.seed
      )
    } else {
      Set.empty[Point]
    }
    
    // Combine all tiles with proper priority:
    // 1. Dungeon tiles (highest priority)
    // 2. Path tiles (override terrain and rivers)
    // 3. River tiles (override terrain)
    // 4. Terrain tiles (base layer)
    val combinedTiles = combineTiles(terrainTiles, riverTiles, pathTiles, dungeons)
    
    WorldMap(
      tiles = combinedTiles,
      dungeons = dungeons,
      rivers = riverTiles,
      paths = pathTiles,
      bounds = config.worldConfig.bounds
    )
  }
  
  /**
   * Combines tiles from different sources with proper priority.
   * Priority: Dungeons > Paths > Rivers > Terrain
   */
  private def combineTiles(
    terrainTiles: Map[Point, TileType],
    riverTiles: Set[Point],
    pathTiles: Set[Point],
    dungeons: Seq[Dungeon]
  ): Map[Point, TileType] = {
    var result = terrainTiles
    
    // Apply rivers (override terrain)
    riverTiles.foreach { point =>
      result = result.updated(point, TileType.Water)
    }
    
    // Apply paths (override terrain and rivers)
    pathTiles.foreach { point =>
      result = result.updated(point, TileType.Dirt)
    }
    
    // Apply dungeon tiles (override everything)
    dungeons.foreach { dungeon =>
      result = result ++ dungeon.tiles
    }
    
    result
  }
  
  /**
   * Verifies that all areas of the map are traversable.
   * Ensures that the player can reach all important locations.
   * 
   * @param worldMap The generated world map
   * @return TraversabilityReport with details about reachability
   */
  def verifyTraversability(worldMap: WorldMap): TraversabilityReport = {
    val walkableTiles = worldMap.tiles.filter { case (_, tileType) =>
      isWalkable(tileType)
    }.keySet
    
    // Find all dungeon entrances
    val dungeonEntrances = worldMap.dungeons.map(_.startPoint).toSet
    
    // Check if we can reach all dungeon entrances from each other
    val reachabilityResults = if (dungeonEntrances.size <= 1) {
      // Single or no dungeon - automatically traversable
      Map(dungeonEntrances.headOption.getOrElse(Point(0, 0)) -> true)
    } else {
      // Check pairwise reachability
      dungeonEntrances.map { start =>
        val reachable = dungeonEntrances.forall { target =>
          if (start == target) true
          else canReach(start, target, walkableTiles)
        }
        start -> reachable
      }.toMap
    }
    
    val allEntrancesReachable = reachabilityResults.values.forall(identity)
    
    TraversabilityReport(
      allEntrancesReachable = allEntrancesReachable,
      walkableTileCount = walkableTiles.size,
      totalTileCount = worldMap.tiles.size,
      dungeonEntrances = dungeonEntrances.toSeq,
      reachabilityMap = reachabilityResults
    )
  }
  
  /**
   * Checks if a tile type is walkable.
   */
  private def isWalkable(tileType: TileType): Boolean = tileType match {
    case TileType.Tree | TileType.Wall | TileType.Rock => false
    case TileType.Water => false // Rivers are not walkable by default
    case _ => true
  }
  
  /**
   * Checks if one point can reach another through walkable tiles.
   * Uses breadth-first search.
   */
  private def canReach(start: Point, target: Point, walkableTiles: Set[Point]): Boolean = {
    if (!walkableTiles.contains(start) || !walkableTiles.contains(target)) {
      return false
    }
    
    val visited = scala.collection.mutable.Set[Point]()
    val queue = scala.collection.mutable.Queue[Point]()
    
    queue.enqueue(start)
    visited += start
    
    while (queue.nonEmpty) {
      val current = queue.dequeue()
      
      if (current == target) {
        return true
      }
      
      // Check all orthogonal neighbors
      val neighbors = Seq(
        Point(current.x + 1, current.y),
        Point(current.x - 1, current.y),
        Point(current.x, current.y + 1),
        Point(current.x, current.y - 1)
      )
      
      neighbors.foreach { neighbor =>
        if (walkableTiles.contains(neighbor) && !visited.contains(neighbor)) {
          visited += neighbor
          queue.enqueue(neighbor)
        }
      }
    }
    
    false
  }
  
  /**
   * Provides a human-readable description of the generated world map.
   */
  def describeWorldMap(worldMap: WorldMap): String = {
    val grassCount = worldMap.tiles.values.count {
      case TileType.Grass1 | TileType.Grass2 | TileType.Grass3 => true
      case _ => false
    }
    val waterCount = worldMap.tiles.values.count(_ == TileType.Water)
    val dirtCount = worldMap.tiles.values.count(_ == TileType.Dirt)
    val treeCount = worldMap.tiles.values.count(_ == TileType.Tree)
    val totalTiles = worldMap.tiles.size
    
    val grassPercent = (grassCount.toDouble / totalTiles * 100).toInt
    val waterPercent = (waterCount.toDouble / totalTiles * 100).toInt
    val dirtPercent = (dirtCount.toDouble / totalTiles * 100).toInt
    val treePercent = (treeCount.toDouble / totalTiles * 100).toInt
    
    val dungeonInfo = worldMap.dungeons.map { dungeon =>
      s"    - ${dungeon.roomGrid.size - dungeon.outdoorRooms.size} rooms at ${dungeon.startPoint}"
    }.mkString("\n")
    
    s"""World Map Generation Summary:
       |  Bounds: ${worldMap.bounds.describe}
       |  Total tiles: $totalTiles
       |  
       |  Terrain Distribution:
       |    Grass: $grassCount tiles ($grassPercent%)
       |    Water (rivers): $waterCount tiles ($waterPercent%)
       |    Dirt (paths): $dirtCount tiles ($dirtPercent%)
       |    Trees: $treeCount tiles ($treePercent%)
       |  
       |  Features:
       |    Rivers: ${worldMap.rivers.size} tiles
       |    Paths: ${worldMap.paths.size} tiles
       |    Dungeons: ${worldMap.dungeons.size}
       |$dungeonInfo""".stripMargin
  }
}

/**
 * Configuration for world map generation.
 * 
 * @param worldConfig Configuration for the base terrain
 * @param dungeonConfigs Configurations for dungeons to place in the world
 * @param riverConfigs Configurations for rivers to generate
 * @param generatePathsToDungeons Whether to generate paths leading to dungeons
 * @param pathsPerDungeon Number of paths leading to each dungeon entrance
 * @param pathWidth Width of paths in tiles
 */
case class WorldMapConfig(
  worldConfig: WorldConfig,
  dungeonConfigs: Seq[DungeonConfig] = Seq.empty,
  riverConfigs: Seq[RiverConfig] = Seq.empty,
  generatePathsToDungeons: Boolean = true,
  pathsPerDungeon: Int = 2,
  pathWidth: Int = 1
)

/**
 * A complete world map with all features.
 * 
 * @param tiles Map from Point to TileType for all tiles
 * @param dungeons All dungeons in the world
 * @param rivers Set of points that are river tiles
 * @param paths Set of points that are path tiles
 * @param bounds The bounds of the world
 */
case class WorldMap(
  tiles: Map[Point, TileType],
  dungeons: Seq[Dungeon],
  rivers: Set[Point],
  paths: Set[Point],
  bounds: MapBounds
)

/**
 * Report on the traversability of a world map.
 * 
 * @param allEntrancesReachable Whether all dungeon entrances can reach each other
 * @param walkableTileCount Number of walkable tiles in the map
 * @param totalTileCount Total number of tiles in the map
 * @param dungeonEntrances All dungeon entrance points
 * @param reachabilityMap Map from each entrance to whether it can reach all others
 */
case class TraversabilityReport(
  allEntrancesReachable: Boolean,
  walkableTileCount: Int,
  totalTileCount: Int,
  dungeonEntrances: Seq[Point],
  reachabilityMap: Map[Point, Boolean]
) {
  /**
   * Provides a human-readable description of the traversability.
   */
  def describe: String = {
    val walkablePercent = (walkableTileCount.toDouble / totalTileCount * 100).toInt
    val status = if (allEntrancesReachable) "✓ PASS" else "✗ FAIL"
    
    val entranceDetails = reachabilityMap.map { case (entrance, reachable) =>
      val statusSymbol = if (reachable) "✓" else "✗"
      s"    $statusSymbol Entrance at $entrance"
    }.mkString("\n")
    
    s"""Traversability Report: $status
       |  Walkable tiles: $walkableTileCount / $totalTileCount ($walkablePercent%)
       |  Dungeon entrances: ${dungeonEntrances.size}
       |  All entrances reachable: $allEntrancesReachable
       |  
       |  Entrance Reachability:
       |$entranceDetails""".stripMargin
  }
}
