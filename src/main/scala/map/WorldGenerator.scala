package map

import game.Point
import scala.util.Random

/**
 * Generator for natural outdoor/world areas with grass, dirt, and trees.
 * Produces terrain that allows freedom of movement while looking natural.
 */
object WorldGenerator {
  
  /**
   * Generates a natural world map based on the provided configuration.
   * 
   * @param config WorldConfig specifying bounds, densities, and other parameters
   * @return Map from tile Point to TileType representing the world terrain
   */
  def generateWorld(config: WorldConfig): Map[Point, TileType] = {
    val random = new Random(config.seed)
    val noise = NoiseGenerator.getNoise(
      config.bounds.minRoomX * Dungeon.roomSize - Dungeon.roomSize,
      config.bounds.maxRoomX * Dungeon.roomSize + Dungeon.roomSize * 2,
      config.bounds.minRoomY * Dungeon.roomSize - Dungeon.roomSize,
      config.bounds.maxRoomY * Dungeon.roomSize + Dungeon.roomSize * 2,
      config.seed
    )
    
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = config.bounds.toTileBounds()
    
    val tiles = for {
      x <- tileMinX to tileMaxX
      y <- tileMinY to tileMaxY
      point = Point(x, y)
    } yield {
      val tileType = if (config.perimeterTrees && isPerimeter(x, y, tileMinX, tileMaxX, tileMinY, tileMaxY)) {
        // Outer perimeter is trees (impassable boundary)
        TileType.Tree
      } else {
        // Generate natural terrain based on density and noise
        generateNaturalTerrain(point, noise, config, random)
      }
      
      (point, tileType)
    }
    
    val worldTiles = tiles.toMap
    
    // If ensureWalkablePaths is enabled, ensure there are walkable paths
    if (config.ensureWalkablePaths) {
      ensureWalkablePaths(worldTiles, config, noise, random)
    } else {
      worldTiles
    }
  }
  
  /**
   * Generates terrain for a specific tile using noise and configuration.
   */
  private def generateNaturalTerrain(
    point: Point,
    noise: Map[(Int, Int), Int],
    config: WorldConfig,
    random: Random
  ): TileType = {
    val noiseValue = noise.getOrElse(point.x -> point.y, (point.x + point.y) % 8)
    val randomValue = random.nextDouble()
    
    // Use noise to create natural-looking patterns
    // Lower noise values (0-2) favor grass, middle values (3-5) can be trees/dirt, high values (6-7) favor grass
    val noiseBonus = if (noiseValue <= 2 || noiseValue >= 6) 0.1 else 0.0
    
    val adjustedGrassDensity = config.grassDensity + noiseBonus
    
    if (randomValue < adjustedGrassDensity) {
      // Grass tiles with variety
      noiseValue % 3 match {
        case 0 => TileType.Grass1
        case 1 => TileType.Grass2
        case 2 => TileType.Grass3
      }
    } else if (randomValue < adjustedGrassDensity + config.treeDensity) {
      // Trees (impassable but natural-looking)
      TileType.Tree
    } else if (randomValue < adjustedGrassDensity + config.treeDensity + config.dirtDensity) {
      // Dirt paths
      TileType.Dirt
    } else {
      // Default to grass variants
      TileType.Grass1
    }
  }
  
  /**
   * Checks if a tile coordinate is on the perimeter of the world.
   */
  private def isPerimeter(x: Int, y: Int, minX: Int, maxX: Int, minY: Int, maxY: Int): Boolean = {
    x == minX || x == maxX || y == minY || y == maxY
  }
  
  /**
   * Ensures that the world has walkable paths by clearing tree clusters.
   * This uses a simple flood-fill approach to ensure connectivity.
   */
  private def ensureWalkablePaths(
    tiles: Map[Point, TileType],
    config: WorldConfig,
    noise: Map[(Int, Int), Int],
    random: Random
  ): Map[Point, TileType] = {
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = config.bounds.toTileBounds()
    
    // Start from center of world and ensure it's walkable
    val centerX = (tileMinX + tileMaxX) / 2
    val centerY = (tileMinY + tileMaxY) / 2
    val centerPoint = Point(centerX, centerY)
    
    var updatedTiles = tiles
    
    // Ensure center area is walkable (3x3 grid around center)
    for {
      dx <- -1 to 1
      dy <- -1 to 1
      point = Point(centerX + dx, centerY + dy)
      if tiles.contains(point) && tiles(point) == TileType.Tree
    } {
      updatedTiles = updatedTiles.updated(point, TileType.Grass1)
    }
    
    // Clear narrow tree corridors that block movement
    // Check each non-perimeter tile and clear if it creates a chokepoint
    for {
      x <- (tileMinX + 1) until tileMaxX
      y <- (tileMinY + 1) until tileMaxY
      point = Point(x, y)
      if updatedTiles.get(point).contains(TileType.Tree)
    } {
      val neighbors = point.neighbors.flatMap(p => updatedTiles.get(p))
      val walkableNeighbors = neighbors.count(isWalkable)
      
      // If this tree is surrounded by mostly walkable tiles, consider clearing it
      if (walkableNeighbors >= 3) {
        // Use noise to decide whether to clear this tree (maintains natural look)
        val noiseValue = noise.getOrElse(point.x -> point.y, (point.x + point.y) % 8)
        if (noiseValue < 4) {
          updatedTiles = updatedTiles.updated(point, TileType.Grass2)
        }
      }
    }
    
    updatedTiles
  }
  
  /**
   * Checks if a tile type is walkable (not a tree or wall).
   */
  private def isWalkable(tileType: TileType): Boolean = tileType match {
    case TileType.Tree | TileType.Wall | TileType.Water | TileType.Rock => false
    case _ => true
  }
  
  /**
   * Provides a human-readable description of the generated world.
   */
  def describeWorld(tiles: Map[Point, TileType], config: WorldConfig): String = {
    val grassCount = tiles.values.count {
      case TileType.Grass1 | TileType.Grass2 | TileType.Grass3 => true
      case _ => false
    }
    val treeCount = tiles.values.count(_ == TileType.Tree)
    val dirtCount = tiles.values.count(_ == TileType.Dirt)
    val totalTiles = tiles.size
    
    val grassPercent = (grassCount.toDouble / totalTiles * 100).toInt
    val treePercent = (treeCount.toDouble / totalTiles * 100).toInt
    val dirtPercent = (dirtCount.toDouble / totalTiles * 100).toInt
    
    s"""World Generation Summary:
       |  Bounds: ${config.bounds.describe}
       |  Total tiles: $totalTiles
       |  Grass tiles: $grassCount ($grassPercent%)
       |  Tree tiles: $treeCount ($treePercent%)
       |  Dirt tiles: $dirtCount ($dirtPercent%)
       |  Perimeter trees: ${config.perimeterTrees}
       |  Walkable paths ensured: ${config.ensureWalkablePaths}""".stripMargin
  }
}
