package map

import game.Point

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
 * Mutator that places dungeons in the world.
 */
class DungeonPlacementMutator(dungeonConfigs: Seq[DungeonConfig]) extends WorldMutator {
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    val dungeons = dungeonConfigs.map(config => DungeonGenerator.generateDungeon(config))
    val dungeonTiles = dungeons.flatMap(_.tiles).toMap
    
    worldMap.copy(
      tiles = worldMap.tiles ++ dungeonTiles,
      dungeons = worldMap.dungeons ++ dungeons
    )
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
    
    val pathTileMap = pathTiles.map(_ -> TileType.Dirt).toMap
    
    worldMap.copy(
      tiles = worldMap.tiles ++ pathTileMap,
      paths = worldMap.paths ++ pathTiles
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
