package map

import game.Point

import scala.collection.immutable
import scala.util.LineOfSight

/**
 * Unified world map generator using extensible mutator pattern.
 * Similar to DungeonGenerator, uses a list of WorldMutators to build the world step by step.
 * This makes world generation extensible and composable.
 */
object WorldMapGenerator {

  /**
   * Calculate dungeon configurations based on world size.
   * For a 21×21 world (-10 to 10), creates 4 dungeons positioned in quadrants.
   * Scales the number of dungeons based on world area.
   *
   * @param worldBounds The bounds of the world
   * @param baseSeed Base seed for dungeon generation
   * @return Sequence of DungeonConfig positioned to avoid overlap
   */
  def calculateDungeonConfigs(worldBounds: MapBounds, baseSeed: Long = System.currentTimeMillis()): Seq[DungeonConfig] = {
    val worldArea = worldBounds.roomArea
    val worldWidth = worldBounds.roomWidth
    val worldHeight = worldBounds.roomHeight
    
    // Calculate number of dungeons based on world size
    // For 21x21 (441 rooms²), we want 4 dungeons
    // Scale: 1 dungeon per 100 rooms²
    val numDungeons = math.max(1, (worldArea / 100.0).round.toInt)
    
    // Position dungeons in a grid pattern (2x2 for 4 dungeons)
    val dungeonsPerRow = math.ceil(math.sqrt(numDungeons)).toInt
    
    // Calculate how much space each dungeon region gets
    val regionWidth = worldWidth / dungeonsPerRow
    val regionHeight = worldHeight / dungeonsPerRow
    
    // Each dungeon should use about 70% of its region, leaving space for terrain
    val dungeonWidth = math.max(7, (regionWidth * 0.7).toInt)  // Min 7 rooms wide
    val dungeonHeight = math.max(7, (regionHeight * 0.7).toInt)  // Min 7 rooms tall
    
    (0 until numDungeons).map { i =>
      val row = i / dungeonsPerRow
      val col = i % dungeonsPerRow
      
      // Calculate center of this region
      val regionCenterX = worldBounds.minRoomX + col * regionWidth + regionWidth / 2
      val regionCenterY = worldBounds.minRoomY + row * regionHeight + regionHeight / 2
      
      // Position dungeon centered in its region
      val minX = regionCenterX - dungeonWidth / 2
      val maxX = regionCenterX + dungeonWidth / 2
      val minY = regionCenterY - dungeonHeight / 2
      val maxY = regionCenterY + dungeonHeight / 2
      
      // Ensure bounds are within world (with margin)
      val clampedMinX = math.max(minX, worldBounds.minRoomX + 1)
      val clampedMaxX = math.min(maxX, worldBounds.maxRoomX - 1)
      val clampedMinY = math.max(minY, worldBounds.minRoomY + 1)
      val clampedMaxY = math.min(maxY, worldBounds.maxRoomY - 1)
      
      // Determine entrance side based on quadrant position
      // Entrances should face toward the center (player spawn at 0,0)
      // This ensures no rooms are placed between the entrance and player area
      val entranceSide = (col, row) match {
        case (0, 0) => game.Direction.Right  // Top-left: face right (toward center)
        case (1, 0) => game.Direction.Left   // Top-right: face left (toward center)
        case (0, 1) => game.Direction.Right  // Bottom-left: face right (toward center)
        case (1, 1) => game.Direction.Left   // Bottom-right: face left (toward center)
        case (c, _) if c < dungeonsPerRow / 2 => game.Direction.Right  // Left side: face right
        case _ => game.Direction.Left  // Right side: face left
      }
      
      DungeonConfig(
        bounds = MapBounds(clampedMinX, clampedMaxX, clampedMinY, clampedMaxY),
        seed = baseSeed + i,
        entranceSide = entranceSide
      )
    }
  }

  /**
   * Generates a complete world map using a list of mutators.
   * Each mutator transforms the world map in sequence.
   *
   * @param config WorldMapConfig specifying all map generation parameters
   * @return WorldMap containing all generated elements
   */
  def generateWorldMap(config: WorldMapConfig): WorldMap = {
    val startPoint = Point(0, 0)
    
    // Use provided dungeon configs or calculate based on world size
    val dungeonConfigs = if (config.dungeonConfigs.nonEmpty) {
      config.dungeonConfigs
    } else {
      calculateDungeonConfigs(config.worldConfig.bounds, config.worldConfig.seed)
    }
    
    // Create list of mutators to apply in sequence
    val mutators: Seq[WorldMutator] = Seq(
      new TerrainMutator(config.worldConfig),
      new DungeonPlacementMutator(dungeonConfigs),
      new ShopPlacementMutator(config.worldConfig.bounds),
      new PathGenerationMutator(startPoint),
      new WalkablePathsMutator(config.worldConfig)
    )
    
    // Start with an empty world map
    val initialWorldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = config.worldConfig.bounds
    )
    
    // Apply each mutator in sequence
    mutators.foldLeft(initialWorldMap) { (worldMap, mutator) =>
      mutator.mutateWorld(worldMap)
    }
  }
  
  /**
   * Generates a world map using custom mutators.
   * Allows for complete customization of the world generation process.
   *
   * @param initialWorldMap The starting world map state
   * @param mutators The list of mutators to apply
   * @return WorldMap after all mutators have been applied
   */
  def generateWorldMapWithMutators(
    initialWorldMap: WorldMap,
    mutators: Seq[WorldMutator]
  ): WorldMap = {
    mutators.foldLeft(initialWorldMap) { (worldMap, mutator) =>
      mutator.mutateWorld(worldMap)
    }
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
 * @param shop Optional shop in the world
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
 * @param shop Optional shop building in the world
 * @param rivers Points that are part of rivers
 * @param paths Points that are part of paths
 * @param bridges Points where bridges cross rivers
 * @param bounds The world map boundaries
 */
case class WorldMap(
  tiles: Map[Point, TileType],
  dungeons: Seq[Dungeon],
  shop: Option[Shop] = None,
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
