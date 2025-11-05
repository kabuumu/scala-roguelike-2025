package map

import game.{Direction, Point}

/**
 * Trait for world generation mutators.
 * Similar to DungeonMutator, each mutator performs a specific transformation on a WorldMap.
 * This allows for extensible, composable world generation.
 */
trait WorldMutator {
  /**
   * Applies this mutator's transformation to the world map.
   * 
   * @param worldMap The current state of the world map
   * @return The transformed world map
   */
  def mutateWorld(worldMap: WorldMap): WorldMap
}

/**
 * Mutator that generates the base terrain (grass, dirt, trees).
 */
class TerrainMutator(config: WorldConfig) extends WorldMutator {
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    val terrainTiles = WorldGenerator.generateWorld(config)
    worldMap.copy(tiles = worldMap.tiles ++ terrainTiles)
  }
}

/**
 * Mutator that generates rivers across the world map.
 * Rivers are placed before dungeons and shops to ensure they don't clash.
 * 
 * @param numRivers Number of rivers to generate
 * @param initialWidth Initial river width (1-5)
 * @param widthVariance Probability of width changing at variance steps
 * @param curveVariance Probability of direction changing at variance steps
 * @param varianceStep Number of tiles between variance changes
 * @param seed Random seed for deterministic generation
 */
class RiverPlacementMutator(
  numRivers: Int,
  initialWidth: Int,
  widthVariance: Double,
  curveVariance: Double,
  varianceStep: Int,
  seed: Long
) extends WorldMutator {
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    val random = new scala.util.Random(seed)
    
    // Generate rivers from different edges
    val riverConfigs = (0 until numRivers).map { i =>
      val edge = i % 4  // Cycle through edges: top, bottom, left, right
      RiverGenerator.createEdgeRiver(
        bounds = worldMap.bounds,
        edge = edge,
        initialWidth = initialWidth,
        widthVariance = widthVariance,
        curveVariance = curveVariance,
        varianceStep = varianceStep,
        seed = seed + i
      )
    }
    
    // Generate all rivers
    val riverPoints = RiverGenerator.generateRivers(riverConfigs)
    
    // Create Water tiles for all river points
    val riverTiles = riverPoints.map(_ -> TileType.Water).toMap
    
    worldMap.copy(
      tiles = worldMap.tiles ++ riverTiles,
      rivers = worldMap.rivers ++ riverPoints
    )
  }
}

/**
 * Mutator that places dungeons in the world.
 * Analyzes the world map to determine optimal dungeon placement.
 * 
 * @param playerStart The player's starting position (default: Point(0,0))
 * @param seed Random seed for deterministic generation
 * @param exclusionRadius Minimum distance in tiles from player start where dungeons cannot be placed
 */
class DungeonPlacementMutator(
  playerStart: Point = Point(0, 0),
  seed: Long = System.currentTimeMillis(),
  exclusionRadius: Int = 10
) extends WorldMutator {
  
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    val dungeonConfigs = analyzeMaplAndCalculateDungeons(worldMap)
    val dungeons = dungeonConfigs.map(config => DungeonGenerator.generateDungeon(config))
    val dungeonTiles = dungeons.flatMap(_.tiles).toMap
    
    worldMap.copy(
      tiles = worldMap.tiles ++ dungeonTiles,
      dungeons = worldMap.dungeons ++ dungeons
    )
  }
  
  /**
   * Analyzes the world map and calculates dungeon configurations.
   * Places dungeons away from player start with varied sizes and entrance orientations.
   */
  private def analyzeMaplAndCalculateDungeons(worldMap: WorldMap): Seq[DungeonConfig] = {
    val bounds = worldMap.bounds
    val worldArea = bounds.roomArea
    val worldWidth = bounds.roomWidth
    val worldHeight = bounds.roomHeight
    
    // Calculate exclusion zone in room coordinates
    val roomSize = 10 // Dungeon.roomSize
    val exclusionRooms = math.ceil(exclusionRadius.toDouble / roomSize).toInt
    
    // Calculate available area excluding the player start region
    val exclusionArea = (2 * exclusionRooms + 1) * (2 * exclusionRooms + 1)
    val availableArea = math.max(0, worldArea - exclusionArea)
    
    // Calculate number of dungeons: 1 per 100 rooms² of available area
    val numDungeons = math.max(1, (availableArea / 100.0).round.toInt)
    
    // Position dungeons in a grid pattern avoiding player start
    val dungeonsPerRow = math.ceil(math.sqrt(numDungeons)).toInt
    
    // Calculate region size for each dungeon
    val regionWidth = worldWidth / dungeonsPerRow
    val regionHeight = worldHeight / dungeonsPerRow
    
    // Convert player start to room coordinates
    val playerRoomX = playerStart.x / roomSize
    val playerRoomY = playerStart.y / roomSize
    
    // Generate dungeon configs with varied sizes
    val random = new scala.util.Random(seed)
    
    val configs = (0 until numDungeons).flatMap { i =>
      val row = i / dungeonsPerRow
      val col = i % dungeonsPerRow
      
      // Calculate center of this region
      val regionCenterX = bounds.minRoomX + col * regionWidth + regionWidth / 2
      val regionCenterY = bounds.minRoomY + row * regionHeight + regionHeight / 2
      
      // Generate varied dungeon size (60-85% of region with some randomness)
      // Use conservative sizing to ensure dungeon generation succeeds
      // For small regions (< 100 rooms²), use even more conservative 60-75%
      val baseSizeRatio = if (regionWidth * regionHeight > 100) 0.7 else 0.6
      val sizeVariation = baseSizeRatio + random.nextDouble() * 0.15
      val dungeonWidth = math.max(7, (regionWidth * sizeVariation).toInt)
      val dungeonHeight = math.max(7, (regionHeight * sizeVariation).toInt)
      
      // Position dungeon centered in region (no random offset for consistency)
      val offsetX = (regionWidth - dungeonWidth) / 2
      val offsetY = (regionHeight - dungeonHeight) / 2
      
      val minX = bounds.minRoomX + col * regionWidth + offsetX
      val maxX = minX + dungeonWidth - 1
      val minY = bounds.minRoomY + row * regionHeight + offsetY
      val maxY = minY + dungeonHeight - 1
      
      // Clamp to world bounds
      val clampedMinX = math.max(minX, bounds.minRoomX)
      val clampedMaxX = math.min(maxX, bounds.maxRoomX)
      val clampedMinY = math.max(minY, bounds.minRoomY)
      val clampedMaxY = math.min(maxY, bounds.maxRoomY)
      
      // Check if the dungeon center is too close to player start
      // For small worlds, we check the center distance rather than full overlap
      val dungeonCenterX = (clampedMinX + clampedMaxX) / 2
      val dungeonCenterY = (clampedMinY + clampedMaxY) / 2
      
      val centerDistance = math.sqrt(
        math.pow(dungeonCenterX - playerRoomX, 2) + 
        math.pow(dungeonCenterY - playerRoomY, 2)
      )
      
      // For very small worlds (< 200 rooms), relax the exclusion requirement
      val effectiveExclusionRooms = if (worldArea < 200) {
        math.max(0, exclusionRooms - 1)  // Reduce exclusion by 1 room for small worlds
      } else {
        exclusionRooms
      }
      
      if (centerDistance < effectiveExclusionRooms) {
        // Skip this dungeon - too close to player
        None
      } else {
        // Determine entrance side based on distance from player start
        // Entrance should face toward player on the dominant axis
        val xDiff = math.abs(dungeonCenterX - playerRoomX)
        val yDiff = math.abs(dungeonCenterY - playerRoomY)
        
        val entranceSide = if (yDiff > xDiff) {
          // Y difference is greater: entrance faces toward player vertically
          if (dungeonCenterY > playerRoomY) Direction.Up    // Dungeon below player: face Up
          else Direction.Down                                // Dungeon above player: face Down
        } else {
          // X difference is greater: entrance faces toward player horizontally
          if (dungeonCenterX > playerRoomX) Direction.Left  // Dungeon right of player: face Left
          else Direction.Right                               // Dungeon left of player: face Right
        }
        
        // Create dungeon config with calculated bounds and entrance orientation
        Some(DungeonConfig(
          bounds = MapBounds(clampedMinX, clampedMaxX, clampedMinY, clampedMaxY),
          seed = seed + i,
          entranceSide = entranceSide
        ))
      }
    }
    
    // If no dungeons were generated (all too close), at least generate one
    if (configs.isEmpty && numDungeons > 0) {
      // For very small worlds, place a single dungeon offset from player
      val offsetX = if (bounds.roomWidth > 5) 3 else 2
      val offsetY = if (bounds.roomHeight > 5) 3 else 2
      
      val dungeonMinX = math.max(bounds.minRoomX, playerRoomX + offsetX)
      val dungeonMaxX = math.min(bounds.maxRoomX, dungeonMinX + math.max(5, bounds.roomWidth / 2))
      val dungeonMinY = math.max(bounds.minRoomY, playerRoomY + offsetY)
      val dungeonMaxY = math.min(bounds.maxRoomY, dungeonMinY + math.max(5, bounds.roomHeight / 2))
      
      Seq(DungeonConfig(
        bounds = MapBounds(dungeonMinX, dungeonMaxX, dungeonMinY, dungeonMaxY),
        seed = seed,
        entranceSide = Direction.Left
      ))
    } else {
      configs
    }
  }
}

/**
 * Mutator that places a shop in the world near the spawn point.
 */
class ShopPlacementMutator(worldBounds: MapBounds) extends WorldMutator {
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    val dungeonBounds = worldMap.dungeons.headOption.map { dungeon =>
      MapBounds(
        dungeon.roomGrid.map(_.x).min,
        dungeon.roomGrid.map(_.x).max,
        dungeon.roomGrid.map(_.y).min,
        dungeon.roomGrid.map(_.y).max
      )
    }.getOrElse(MapBounds(0, 0, 0, 0))
    
    val shopLocation = Shop.findShopLocation(dungeonBounds, worldBounds)
    val shop = Shop(shopLocation)
    
    worldMap.copy(
      tiles = worldMap.tiles ++ shop.tiles,
      shop = Some(shop)
    )
  }
}

/**
 * Mutator that creates dirt paths between key locations (spawn, dungeons, shops).
 * If paths cross rivers, bridges are placed instead of dirt tiles.
 */
class PathGenerationMutator(startPoint: Point) extends WorldMutator {
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    import scala.util.LineOfSight
    
    // Find all dungeon entrances and shop entrance
    val dungeonEntrances = worldMap.dungeons.map(_.startPoint).map(Dungeon.roomToTile)
    val destinations = worldMap.shop.map(_.entranceTile).toSeq ++ dungeonEntrances
    
    // Create paths from start point to all destinations
    val pathTiles: Set[Point] = (for {
      destination <- destinations
      pathPoint <- LineOfSight.getBresenhamLine(startPoint, destination)
    } yield pathPoint).toSet
    
    // Separate path tiles into those on water (need bridges) and those on land (need dirt)
    val pathsOnWater = pathTiles.intersect(worldMap.rivers)
    val pathsOnLand = pathTiles -- pathsOnWater
    
    // Create Bridge tiles for paths crossing rivers, Dirt tiles for other paths
    val bridgeTiles = pathsOnWater.map(_ -> TileType.Bridge).toMap
    val dirtTiles = pathsOnLand.map(_ -> TileType.Dirt).toMap
    val pathTileMap = bridgeTiles ++ dirtTiles
    
    worldMap.copy(
      tiles = worldMap.tiles ++ pathTileMap,
      paths = worldMap.paths ++ pathTiles,
      bridges = worldMap.bridges ++ pathsOnWater
    )
  }
}

/**
 * Mutator that ensures walkable paths by clearing tree clusters.
 * This is an optional mutator that can be applied if needed.
 */
class WalkablePathsMutator(config: WorldConfig) extends WorldMutator {
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    // Only apply if the config enables walkable paths
    if (!config.ensureWalkablePaths) {
      return worldMap
    }
    
    // This mutator would modify tiles to ensure connectivity
    // For now, the terrain generator already handles this
    // But this could be enhanced in the future
    worldMap
  }
}
