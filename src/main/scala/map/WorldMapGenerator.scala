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
   * Generates a complete world map using a list of mutators.
   * Each mutator transforms the world map in sequence.
   *
   * @param config WorldMapConfig specifying all map generation parameters
   * @return WorldMap containing all generated elements
   */
  def generateWorldMap(config: WorldMapConfig): WorldMap = {
    val startPoint = Point(0, 0)
    
    // Create list of mutators to apply in sequence
    // DungeonPlacementMutator now analyzes the world map internally
    val mutators: Seq[WorldMutator] = Seq(
      new TerrainMutator(config.worldConfig),
      new DungeonPlacementMutator(
        playerStart = startPoint,
        seed = config.worldConfig.seed,
        exclusionRadius = 10
      ),
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
 */
case class WorldMapConfig(
  worldConfig: WorldConfig
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
