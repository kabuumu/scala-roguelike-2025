package map

import game.Point

import scala.collection.immutable
import scala.util.LineOfSight

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

    // Currently hardcoded to a single dungeon for ease
    val dungeons = Seq(DungeonGenerator.generateDungeon(MapBounds(
      minRoomX = 1,
      maxRoomX = config.worldConfig.bounds.maxRoomX,
      minRoomY = config.worldConfig.bounds.minRoomY,
      maxRoomY = config.worldConfig.bounds.maxRoomY
    ), seed = 182L))

    val startPoint = Point(0, 0)

    // Find dungeon entrance points (use the actual dungeon start points, not outdoor rooms)
    val dungeonEntrances = dungeons.map(_.startPoint).map(Dungeon.roomToTile)

    // ===== PASS 3: Create dirt paths between all dungeons =====
    val pathTiles: Set[Point] = (for {
      dungeonEntrance <- dungeonEntrances
      pathPoint <- LineOfSight.getBresenhamLine(startPoint, dungeonEntrance)
    } yield pathPoint).toSet

    // Combine all tiles with proper priority:
    // Dungeons > Bridges > Paths > Rivers > Terrain
    val combinedTiles: Map[Point, TileType] = Map.empty ++ terrainTiles ++ dungeons.flatMap(_.tiles) ++ (pathTiles.map(_ -> TileType.Dirt))

    WorldMap(
      tiles = combinedTiles,
      dungeons = dungeons,
      rivers = Set.empty,
      paths = pathTiles,
      bridges = Set.empty,
      bounds = config.worldConfig.bounds
    )
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
