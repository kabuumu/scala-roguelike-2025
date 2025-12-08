package map

import game.Point
import scala.util.Random

/**
 * Represents a village - a collection of buildings in a clustered area.
 * 
 * @param buildings The buildings that make up this village (3-5 buildings)
 * @param centerLocation The approximate center of the village
 */
case class Village(
  buildings: Seq[Building],
  centerLocation: Point
) {
  require(buildings.length >= 3 && buildings.length <= 5, "Village must have 3-5 buildings")
  
  /**
   * All tiles from all buildings in the village.
   * When buildings overlap, walls take precedence over floors.
   */
  lazy val tiles: Map[Point, TileType] = {
    val allTiles = buildings.flatMap(_.tiles)
    // Group by point and select walls over floors when there are conflicts
    allTiles.groupBy(_._1).map { case (point, tiles) =>
      val tileTypes = tiles.map(_._2)
      // Prefer walls over floors
      val finalType = if (tileTypes.contains(TileType.Wall)) {
        TileType.Wall
      } else {
        tileTypes.headOption.getOrElse(TileType.Floor) // Default to Floor if empty (shouldn't happen)
      }
      point -> finalType
    }
  }
  
  /**
   * All wall points from all buildings.
   */
  lazy val walls: Set[Point] = buildings.flatMap(_.walls).toSet
  
  /**
   * The shop building in this village (helper for backward compatibility).
   */
  lazy val shopBuilding: Building = buildings.find(_.isShop).getOrElse(buildings.head)
  
  /**
   * Get all entrance tiles for all buildings.
   */
  def entrances: Seq[Point] = buildings.map(_.entranceTile)
}

object Village {
  /**
   * Generate a village at a specific location.
   * Creates 3-5 buildings clustered around a center point.
   * 
   * @param centerLocation Center location in tile coordinates
   * @param seed Random seed for deterministic generation
   * @return A Village instance
   */
  def generateVillage(centerLocation: Point, seed: Long): Village = {
    val random = new Random(seed)
    
    // Determine number of buildings (3-5)
    val numBuildings = 3 + random.nextInt(3)  // 3, 4, or 5
    
    // Generate buildings in a cluster pattern
    val buildingTypes = random.shuffle(Seq(
      BuildingType.Healer,
      BuildingType.PotionShop,
      BuildingType.EquipmentShop
    ) ++ Seq.fill(numBuildings - 3)(BuildingType.Generic))

    val buildings = (0 until numBuildings).map { i =>
      // Create varied building sizes (5-10 internal space)
      val width = 5 + random.nextInt(6)   // 5-10
      val height = 5 + random.nextInt(6)  // 5-10
      
      // Position buildings in a rough grid around center
      // Use a simple layout: 
      // For 3 buildings: arrange in triangle
      // For 4 buildings: arrange in 2x2 grid
      // For 5 buildings: arrange in cross pattern
      val offset = i match {
        case 0 => Point(-15, -15)  // Top-left
        case 1 => Point(5, -15)    // Top-right
        case 2 => Point(-15, 5)    // Bottom-left
        case 3 => Point(5, 5)      // Bottom-right (if 4+ buildings)
        case 4 => Point(-5, -5)    // Center (if 5 buildings)
      }
      
      val buildingLocation = Point(
        centerLocation.x + offset.x,
        centerLocation.y + offset.y
      )
      
      Building(
        location = buildingLocation,
        width = width,
        height = height,
        buildingType = buildingTypes(i)
      )
    }
    
    Village(buildings, centerLocation)
  }
  
  /**
   * Find a suitable location for a village that doesn't conflict with dungeon.
   * Similar to Shop.findShopLocation but for villages.
   * 
   * @param dungeonBounds The bounds of dungeons to avoid
   * @param worldBounds The bounds of the entire world
   * @param preferredDistance Preferred distance from origin in tiles
   * @return A Point in tile coordinates suitable for village placement
   */
  def findVillageLocation(
    dungeonBounds: Seq[MapBounds],
    worldBounds: MapBounds,
    preferredDistance: Int = 30
  ): Point = {
    // Convert world bounds to tile coordinates
    val (worldMinX, worldMaxX, worldMinY, worldMaxY) = worldBounds.toTileBounds(10)
    
    // Try locations at increasing distances from origin, with more granular steps
    val distances = (30 to 150 by 20).toSeq
    
    val allValidCandidates = distances.flatMap { dist =>
      // Try 16 directions around a circle for better coverage
      val angles = (0 until 16).map(_ * (2 * math.Pi / 16))
      val candidates = angles.map { angle =>
        Point(
          (dist * math.cos(angle)).toInt,
          (dist * math.sin(angle)).toInt
        )
      }
      
      // Filter candidates that are:
      // 1. Within world bounds (with margin for village size ~40 tiles)
      // 2. Not overlapping with any dungeon bounds (with sufficient padding)
      candidates.filter { candidate =>
        val villageSize = 40  // Approximate size of village
        val withinWorld = 
          candidate.x - villageSize >= worldMinX && 
          candidate.x + villageSize <= worldMaxX &&
          candidate.y - villageSize >= worldMinY && 
          candidate.y + villageSize <= worldMaxY
        
        val farFromAllDungeons = if (dungeonBounds.isEmpty) {
          true
        } else {
          dungeonBounds.forall { bounds =>
            val (dungeonMinX, dungeonMaxX, dungeonMinY, dungeonMaxY) = bounds.toTileBounds(10)
            // Use village size as padding to ensure complete separation
            val padding = villageSize + 10
            // Village bounds check: no part of the village overlaps with dungeon + padding
            val villageMinX = candidate.x - villageSize
            val villageMaxX = candidate.x + villageSize
            val villageMinY = candidate.y - villageSize
            val villageMaxY = candidate.y + villageSize
            
            // Check if rectangles don't overlap (with padding)
            villageMaxX < dungeonMinX - padding || 
            villageMinX > dungeonMaxX + padding ||
            villageMaxY < dungeonMinY - padding || 
            villageMinY > dungeonMaxY + padding
          }
        }
        
        withinWorld && farFromAllDungeons
      }
    }
    
    // Return first valid candidate, or try without as much padding for fallback
    allValidCandidates.headOption.getOrElse {
      // Fallback: try to place far from dungeons with less strict requirements
      val farCorners = Seq(
        Point(worldMaxX - 50, worldMaxY - 50),
        Point(worldMinX + 50, worldMaxY - 50),
        Point(worldMaxX - 50, worldMinY + 50),
        Point(worldMinX + 50, worldMinY + 50)
      )
      
      farCorners.headOption.getOrElse(Point(0, 0))
    }
  }
}
