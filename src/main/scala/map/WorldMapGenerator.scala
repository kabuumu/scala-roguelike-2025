package map

import game.Point

import scala.collection.immutable
import scala.util.LineOfSight

/** Unified world map generator using extensible mutator pattern. Similar to
  * DungeonGenerator, uses a list of WorldMutators to build the world step by
  * step. This makes world generation extensible and composable.
  */
object WorldMapGenerator {

  /** Generates a complete world map using a list of mutators. Each mutator
    * transforms the world map in sequence.
    *
    * @param config
    *   WorldMapConfig specifying all map generation parameters
    * @return
    *   WorldMap containing all generated elements
    */
  def generateWorldMap(config: WorldMapConfig): WorldMap = {
    val startPoint = Point(0, 0)

    // Create list of mutators to apply in sequence
    // Spawn village is placed FIRST (after terrain/rivers) so dungeons avoid it
    // Rivers are placed BEFORE dungeons and villages to avoid clashing
    // Paths are placed AFTER rivers so bridges can be placed on water tiles
    val mutators: Seq[WorldMutator] = Seq(
      new TerrainMutator(config.worldConfig),
      new RegionFeaturesMutator(config.worldConfig)
    )

    // Start with an empty world map
    val initialWorldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      shop = None,
      villages = Seq.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = config.worldConfig.bounds,
      chunks = Map.empty,
      seed = config.worldConfig.seed,
      processedRegions = Set.empty,
      lastCenterChunk = None
    )

    // Apply each mutator in sequence
    mutators.foldLeft(initialWorldMap) { (worldMap, mutator) =>
      mutator.mutateWorld(worldMap)
    }
  }
}

/** Configuration for world map generation.
  *
  * @param worldConfig
  *   Configuration for the base terrain
  * @param numRivers
  *   Number of rivers to generate (default: 2)
  * @param riverWidth
  *   Initial river width 1-5 (default: 2)
  * @param riverWidthVariance
  *   Probability of width changing at variance steps (default: 0.3)
  * @param riverCurveVariance
  *   Probability of direction changing at variance steps (default: 0.4)
  * @param riverVarianceStep
  *   Number of tiles between variance changes (default: 3)
  * @param numDungeons
  *   Number of dungeons to generate (None = auto-calculate based on world size)
  * @param numVillages
  *   Number of villages to generate (default: 1)
  */
case class WorldMapConfig(
    worldConfig: WorldConfig,
    numDungeons: Option[Int] = None,
    numVillages: Int = 1
) {
  require(numVillages >= 0, "numVillages must be non-negative")
  require(
    numDungeons.isEmpty || numDungeons.get >= 0,
    "numDungeons must be non-negative if specified"
  )
}

/** A complete world map with all features.
  *
  * @param tiles
  *   Map from Point to TileType for all tiles
  * @param dungeons
  *   All dungeons in the world
  * @param shop
  *   Optional shop in the world
  * @param rivers
  *   Set of points that are river tiles
  * @param paths
  *   Set of points that are path tiles
  * @param bridges
  *   Set of points where bridges cross rivers
  * @param bounds
  *   The bounds of the world
  */
/** Unified world map that combines all terrain elements and provides consistent
  * blocking behavior. Replaces the split between Dungeon and worldTiles in
  * GameState.
  *
  * @param tiles
  *   All tiles in the world (terrain, dungeons, rivers, paths, etc.)
  * @param dungeons
  *   The dungeon structures included in this world (for spawning, items, etc.)
  * @param shop
  *   Optional shop building in the world (deprecated, use villages instead)
  * @param villages
  *   Collection of villages in the world
  * @param rivers
  *   Points that are part of rivers
  * @param paths
  *   Points that are part of paths
  * @param bridges
  *   Points where bridges cross rivers
  * @param bounds
  *   The world map boundaries
  */
case class WorldMap(
    tiles: Map[Point, TileType],
    dungeons: Seq[Dungeon],
    shop: Option[Shop] = None,
    villages: Seq[Village] = Seq.empty,
    // rivers: Set[Point] removed - use tiles(p) == TileType.Water check
    paths: Set[Point],
    bridges: Set[Point],
    bounds: MapBounds,
    chunks: Map[(Int, Int), Chunk] = Map.empty,
    seed: Long = 0L,
    processedRegions: Set[(Int, Int)] = Set.empty,
    lastCenterChunk: Option[(Int, Int)] = None
) {
  def getTile(point: Point): Option[TileType] = tiles.get(point)

  /** Computed tile sets for efficient lookups. Single pass through tiles map to
    * extract all relevant sets.
    */
  private lazy val tileSets: (Set[Point], Set[Point], Set[Point]) = {
    val wallsBuilder = Set.newBuilder[Point]
    val rocksBuilder = Set.newBuilder[Point]
    val waterBuilder = Set.newBuilder[Point]

    tiles.foreach { case (point, tileType) =>
      tileType match {
        case TileType.Wall                 => wallsBuilder += point
        case TileType.Rock | TileType.Tree => rocksBuilder += point
        case TileType.Water                => waterBuilder += point
        case _                             => // ignore other tile types
      }
    }

    (wallsBuilder.result(), rocksBuilder.result(), waterBuilder.result())
  }

  /** Points that block line of sight (walls).
    */
  lazy val walls: Set[Point] = tileSets._1

  /** Points that are rocks or trees (impassable terrain features).
    */
  lazy val rocks: Set[Point] = tileSets._2

  /** Points that are water (impassable unless bridged). Bridges make water
    * passable, so we exclude bridge points.
    */
  lazy val water: Set[Point] = tileSets._3 -- bridges

  /** Pre-calculated set of points that block line of sight (walls only). Used
    * by GameState to avoid re-calculating this set every frame.
    */
  lazy val staticLineOfSightBlockingPoints: Set[Point] = walls

  /** Pre-calculated set of points that block movement (walls + water + rocks).
    * Used by GameState to avoid re-calculating this set every frame.
    */
  lazy val staticMovementBlockingPoints: Set[Point] = walls ++ water ++ rocks

  /** Get the primary dungeon (first dungeon if multiple exist). Maintains
    * backward compatibility with code expecting a single dungeon.
    */
  def primaryDungeon: Option[Dungeon] = dungeons.headOption

  /** All points where items can be found (from all dungeons).
    */
  def allItems: Set[(Point, data.Items.ItemReference)] =
    dungeons.flatMap(_.items).toSet

  /** All trader room locations (from all dungeons).
    */
  def allTraderRooms: Seq[Point] = dungeons.flatMap(_.traderRoom)
}
