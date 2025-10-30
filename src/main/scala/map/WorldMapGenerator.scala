package map

import game.Point

/**
 * Unified world map that combines terrain, rivers, paths, and dungeons.
 * Uses a 4-pass approach for natural procedural generation:
 * 1st pass - Create grass/dirt based on noise
 * 2nd pass - Place dungeons with spacing
 * 3rd pass - Create paths between dungeons
 * 4th pass - Create rivers with bridges at path crossings
 */
object WorldMapGenerator {
  
  /**
   * Generates a complete world map with terrain, rivers, paths, and dungeons.
   * Uses multi-pass approach for procedural generation.
   * 
   * @param config WorldMapConfig specifying all map generation parameters
   * @return WorldMap containing all generated elements
   */
  def generateWorldMap(config: WorldMapConfig): WorldMap = {
    // ===== PASS 1: Create base terrain (grass/dirt/trees) =====
    val terrainTiles = WorldGenerator.generateWorld(config.worldConfig)
    
    // ===== PASS 2: Place dungeons on the world with spacing =====
    val (dungeons, dungeonPlacements) = placeDungeonsWithSpacing(
      config.dungeonConfigs,
      config.worldConfig.bounds,
      config.minDungeonSpacing
    )
    
    // Find dungeon entrance points (use the actual dungeon start points, not outdoor rooms)
    val dungeonEntrances = dungeons.map(_.startPoint)
    
    // ===== PASS 3: Create dirt paths between all dungeons =====
    val pathTiles = if (dungeonEntrances.size > 1 && config.generatePathsBetweenDungeons) {
      generatePathsBetweenDungeons(dungeonEntrances.toSeq, config.pathWidth)
    } else if (dungeonEntrances.nonEmpty && config.generatePathsToDungeons) {
      // Fallback: paths from world edges to dungeons
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
    
    // ===== PASS 4: Create rivers with bridges at path crossings =====
    val (riverTiles, bridgeTiles) = if (config.riverConfigs.nonEmpty) {
      val rivers = RiverGenerator.generateRivers(config.riverConfigs)
      val bridges = findBridgePoints(rivers, pathTiles)
      (rivers, bridges)
    } else {
      (Set.empty[Point], Set.empty[Point])
    }
    
    // Combine all tiles with proper priority:
    // Dungeons > Bridges > Paths > Rivers > Terrain
    val combinedTiles = combineTilesWithBridges(
      terrainTiles, riverTiles, pathTiles, bridgeTiles, dungeons
    )
    
    WorldMap(
      tiles = combinedTiles,
      dungeons = dungeons,
      rivers = riverTiles,
      paths = pathTiles,
      bridges = bridgeTiles,
      bounds = config.worldConfig.bounds
    )
  }
  
  /**
   * Places dungeons on the world map ensuring minimum spacing between them.
   * Returns dungeons without the hardcoded outdoor rooms.
   */
  private def placeDungeonsWithSpacing(
    configs: Seq[DungeonConfig],
    bounds: MapBounds,
    minSpacing: Int
  ): (Seq[Dungeon], Seq[Point]) = {
    val random = new scala.util.Random(configs.headOption.map(_.seed).getOrElse(System.currentTimeMillis()))
    val placements = scala.collection.mutable.ArrayBuffer[Point]()
    
    val dungeons = configs.map { config =>
      // Find a suitable placement that doesn't overlap with existing dungeons
      val placement = findDungeonPlacement(placements.toSeq, bounds, minSpacing, random)
      placements += placement
      
      // Generate dungeon using standard generation (WITH outdoor rooms)
      // The dungeon will be placed on top of the world terrain
      val baseDungeon = MapGenerator.generateDungeon(config.size, config.lockedDoorCount, config.itemCount, config.seed)
      
      // Shift dungeon to the placement location
      shiftDungeon(baseDungeon, placement)
    }
    
    (dungeons, placements.toSeq)
  }
  
  /**
   * Finds a suitable placement for a dungeon that maintains spacing from existing dungeons.
   */
  private def findDungeonPlacement(
    existingPlacements: Seq[Point],
    bounds: MapBounds,
    minSpacing: Int,
    random: scala.util.Random
  ): Point = {
    val maxAttempts = 100
    var attempt = 0
    
    while (attempt < maxAttempts) {
      val x = random.between(bounds.minRoomX, bounds.maxRoomX + 1)
      val y = random.between(bounds.minRoomY, bounds.maxRoomY + 1)
      val candidate = Point(x, y)
      
      // Check if this placement maintains minimum spacing from all existing dungeons
      val hasGoodSpacing = existingPlacements.forall { existing =>
        val distance = math.abs(candidate.x - existing.x) + math.abs(candidate.y - existing.y)
        distance >= minSpacing
      }
      
      if (hasGoodSpacing) {
        return candidate
      }
      
      attempt += 1
    }
    
    // If we couldn't find a good placement, return a random one anyway
    Point(
      random.between(bounds.minRoomX, bounds.maxRoomX + 1),
      random.between(bounds.minRoomY, bounds.maxRoomY + 1)
    )
  }
  
  /**
   * Shifts a dungeon to a new location.
   */
  private def shiftDungeon(dungeon: Dungeon, targetCenter: Point): Dungeon = {
    // Calculate current center
    val currentMinX = dungeon.roomGrid.map(_.x).min
    val currentMaxX = dungeon.roomGrid.map(_.x).max
    val currentMinY = dungeon.roomGrid.map(_.y).min
    val currentMaxY = dungeon.roomGrid.map(_.y).max
    
    val currentCenterX = (currentMinX + currentMaxX) / 2
    val currentCenterY = (currentMinY + currentMaxY) / 2
    
    val shiftX = targetCenter.x - currentCenterX
    val shiftY = targetCenter.y - currentCenterY
    
    dungeon.copy(
      roomGrid = dungeon.roomGrid.map(p => Point(p.x + shiftX, p.y + shiftY)),
      startPoint = Point(dungeon.startPoint.x + shiftX, dungeon.startPoint.y + shiftY),
      endpoint = dungeon.endpoint.map(p => Point(p.x + shiftX, p.y + shiftY)),
      traderRoom = dungeon.traderRoom.map(p => Point(p.x + shiftX, p.y + shiftY)),
      roomConnections = dungeon.roomConnections.map(rc => 
        rc.copy(
          originRoom = Point(rc.originRoom.x + shiftX, rc.originRoom.y + shiftY),
          destinationRoom = Point(rc.destinationRoom.x + shiftX, rc.destinationRoom.y + shiftY)
        )
      ),
      items = dungeon.items.map { case (p, item) => (Point(p.x + shiftX, p.y + shiftY), item) }
    )
  }
  
  /**
   * Generates paths that connect all dungeons to each other.
   * Creates a network of dirt paths between dungeon entrances.
   */
  private def generatePathsBetweenDungeons(
    entrances: Seq[Point],
    pathWidth: Int
  ): Set[Point] = {
    if (entrances.size < 2) return Set.empty
    
    val allPaths = scala.collection.mutable.Set[Point]()
    
    // Connect each dungeon to its nearest neighbor(s) to create a network
    entrances.zipWithIndex.foreach { case (entrance, idx) =>
      // Find the nearest other entrance
      val others = entrances.zipWithIndex.filter(_._2 != idx).map(_._1)
      val nearest = others.minBy { other =>
        val dx = entrance.x - other.x
        val dy = entrance.y - other.y
        dx * dx + dy * dy
      }
      
      // Create path to nearest entrance
      val tileStart = Point(entrance.x * Dungeon.roomSize + Dungeon.roomSize / 2,
                           entrance.y * Dungeon.roomSize + Dungeon.roomSize / 2)
      val tileTarget = Point(nearest.x * Dungeon.roomSize + Dungeon.roomSize / 2,
                            nearest.y * Dungeon.roomSize + Dungeon.roomSize / 2)
      
      allPaths ++= PathGenerator.generatePath(tileStart, tileTarget, pathWidth, 
        MapBounds(-1000, 1000, -1000, 1000)) // Use large bounds
    }
    
    allPaths.toSet
  }
  
  /**
   * Finds points where rivers cross paths and should have bridges.
   */
  private def findBridgePoints(rivers: Set[Point], paths: Set[Point]): Set[Point] = {
    rivers.intersect(paths)
  }
  
  /**
   * Combines tiles from different sources with proper priority.
   * Priority: Dungeons > Bridges > Paths > Rivers > Terrain
   */
  private def combineTilesWithBridges(
    terrainTiles: Map[Point, TileType],
    riverTiles: Set[Point],
    pathTiles: Set[Point],
    bridgeTiles: Set[Point],
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
    
    // Apply bridges (override rivers at path crossings)
    bridgeTiles.foreach { point =>
      result = result.updated(point, TileType.Bridge)
    }
    
    // Apply dungeon tiles (override everything)
    // But EXCLUDE outdoor room tiles - let world terrain show through instead
    dungeons.foreach { dungeon =>
      // Add dungeon tiles directly to the result
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
      s"    - ${dungeon.roomGrid.size} rooms at ${dungeon.startPoint}"
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
 * @param generatePathsToDungeons Whether to generate paths leading to dungeons from edges
 * @param generatePathsBetweenDungeons Whether to generate paths connecting all dungeons
 * @param pathsPerDungeon Number of paths leading to each dungeon entrance (when using edge paths)
 * @param pathWidth Width of paths in tiles
 * @param minDungeonSpacing Minimum spacing between dungeon centers (in rooms)
 */
case class WorldMapConfig(
  worldConfig: WorldConfig,
  dungeonConfigs: Seq[DungeonConfig] = Seq.empty,
  riverConfigs: Seq[RiverConfig] = Seq.empty,
  generatePathsToDungeons: Boolean = true,
  generatePathsBetweenDungeons: Boolean = true,
  pathsPerDungeon: Int = 2,
  pathWidth: Int = 1,
  minDungeonSpacing: Int = 10
)

/**
 * A complete world map with all features.
 * 
 * @param tiles Map from Point to TileType for all tiles
 * @param dungeons All dungeons in the world
 * @param rivers Set of points that are river tiles
 * @param paths Set of points that are path tiles
 * @param bridges Set of points where bridges cross rivers
 * @param bounds The bounds of the world
 */
/**
 * Unified world map that combines all terrain elements and provides consistent blocking behavior.
 * Replaces the split between Dungeon and worldTiles in GameState.
 * 
 * @param tiles All tiles in the world (terrain, dungeons, rivers, paths, etc.)
 * @param dungeons The dungeon structures included in this world (for spawning, items, etc.)
 * @param rivers Points that are part of rivers
 * @param paths Points that are part of paths
 * @param bridges Points where bridges cross rivers
 * @param bounds The world map boundaries
 */
case class WorldMap(
  tiles: Map[Point, TileType],
  dungeons: Seq[Dungeon],
  rivers: Set[Point],
  paths: Set[Point],
  bridges: Set[Point],
  bounds: MapBounds
) {
  /**
   * Points that block line of sight (walls and trees).
   * Trees block sight in open world areas.
   */
  lazy val walls: Set[Point] = tiles.filter { case (_, tileType) =>
    tileType == TileType.Wall || tileType == TileType.Tree
  }.keySet
  
  /**
   * Points that are rocks (impassable terrain features).
   */
  lazy val rocks: Set[Point] = tiles.filter(_._2 == TileType.Rock).keySet
  
  /**
   * Points that are water (impassable unless bridged).
   * Bridges make water passable, so we exclude bridge points.
   */
  lazy val water: Set[Point] = tiles.filter(_._2 == TileType.Water).keySet -- bridges
  
  /**
   * Get the primary dungeon (first dungeon if multiple exist).
   * Maintains backward compatibility with code expecting a single dungeon.
   */
  def primaryDungeon: Option[Dungeon] = dungeons.headOption
  
  /**
   * All points where items can be found (from all dungeons).
   */
  def allItems: Set[(Point, data.Items.ItemReference)] = dungeons.flatMap(_.items).toSet
  
  /**
   * All trader room locations (from all dungeons).
   */
  def allTraderRooms: Seq[Point] = dungeons.flatMap(_.traderRoom)
}

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
