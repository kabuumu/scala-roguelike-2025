package map

import game.Point
import map.TileType._

/**
 * Represents a shop building in the world.
 * A simple square building with brick walls and a trader inside.
 * 
 * @param location The room coordinate where the shop is located
 * @param size Size of the shop in tiles (shop will be size x size)
 */
case class Shop(
  location: Point,
  size: Int = 10
) {
  
  /**
   * Calculate the center tile position of the shop.
   * This is where the trader will be placed.
   */
  def centerTile: Point = {
    Point(
      location.x * size + size / 2,
      location.y * size + size / 2
    )
  }
  
  /**
   * Calculate the entrance tile position (middle of one wall).
   * The entrance faces the origin (0, 0) for easier access.
   */
  def entranceTile: Point = {
    val baseX = location.x * size
    val baseY = location.y * size
    
    // Determine which side to place entrance based on location relative to origin
    if (location.x >= 0 && location.y >= 0) {
      // Shop is in quadrant I (bottom-right) - entrance on left or top
      if (location.x > location.y) {
        // More to the right, entrance on left
        Point(baseX, baseY + size / 2)
      } else {
        // More up, entrance on top
        Point(baseX + size / 2, baseY)
      }
    } else if (location.x < 0 && location.y >= 0) {
      // Shop is in quadrant II (bottom-left) - entrance on right or top
      if (-location.x > location.y) {
        // More to the left, entrance on right
        Point(baseX + size, baseY + size / 2)
      } else {
        // More up, entrance on top
        Point(baseX + size / 2, baseY)
      }
    } else if (location.x < 0 && location.y < 0) {
      // Shop is in quadrant III (top-left) - entrance on right or bottom
      if (-location.x > -location.y) {
        // More to the left, entrance on right
        Point(baseX + size, baseY + size / 2)
      } else {
        // More up, entrance on bottom
        Point(baseX + size / 2, baseY + size)
      }
    } else {
      // Shop is in quadrant IV (top-right) - entrance on left or bottom
      if (location.x > -location.y) {
        // More to the right, entrance on left
        Point(baseX, baseY + size / 2)
      } else {
        // More down, entrance on bottom
        Point(baseX + size / 2, baseY + size)
      }
    }
  }
  
  /**
   * Generate all tiles for the shop.
   * Creates a square building with brick walls and floor inside.
   */
  lazy val tiles: Map[Point, TileType] = {
    val baseX = location.x * size
    val baseY = location.y * size
    val entrance = entranceTile
    
    val shopTiles = for {
      x <- baseX to (baseX + size)
      y <- baseY to (baseY + size)
    } yield {
      val point = Point(x, y)
      
      // Determine if this is a wall position
      val isWall = x == baseX || x == baseX + size || y == baseY || y == baseY + size
      
      // Is this the entrance?
      val isEntrance = point == entrance
      
      val tileType = if (isWall && !isEntrance) {
        TileType.Wall  // Brick walls
      } else {
        TileType.Floor  // Floor inside
      }
      
      point -> tileType
    }
    
    shopTiles.toMap
  }
  
  /**
   * All wall points in the shop.
   */
  lazy val walls: Set[Point] = tiles.filter(_._2 == TileType.Wall).keySet
}

object Shop {
  /**
   * Convert room coordinates to tile coordinates (center of room).
   */
  def roomToTile(room: Point, size: Int = 10): Point = {
    Point(
      room.x * size + size / 2,
      room.y * size + size / 2
    )
  }
  
  /**
   * Find a suitable location for a shop near the origin that doesn't conflict with dungeon.
   * 
   * @param dungeonBounds The bounds of the dungeon to avoid
   * @param worldBounds The bounds of the entire world
   * @param preferredDistance Preferred distance from origin in rooms
   * @return A Point in room coordinates suitable for shop placement
   */
  def findShopLocation(
    dungeonBounds: MapBounds,
    worldBounds: MapBounds,
    preferredDistance: Int = 3
  ): Point = {
    // Try locations at increasing distances from origin
    // Prefer locations that are away from dungeon
    
    // Start with preferred distance, then try further out if needed
    val distances = Seq(preferredDistance, preferredDistance + 2, preferredDistance + 4, preferredDistance + 6)
    
    val allValidCandidates = distances.flatMap { dist =>
      val candidates = Seq(
        Point(dist, 0),      // Right of origin
        Point(-dist, 0),     // Left of origin
        Point(0, dist),      // Below origin
        Point(0, -dist),     // Above origin
        Point(dist, dist),    // Bottom-right
        Point(-dist, dist),   // Bottom-left
        Point(-dist, -dist),  // Top-left
        Point(dist, -dist)    // Top-right
      )
      
      // Filter candidates that are:
      // 1. Within world bounds
      // 2. Not overlapping with dungeon bounds (with 2 room padding)
      candidates.filter { candidate =>
        val withinWorld = 
          candidate.x >= worldBounds.minRoomX && candidate.x <= worldBounds.maxRoomX &&
          candidate.y >= worldBounds.minRoomY && candidate.y <= worldBounds.maxRoomY
        
        val farFromDungeon = 
          candidate.x < dungeonBounds.minRoomX - 2 || candidate.x > dungeonBounds.maxRoomX + 2 ||
          candidate.y < dungeonBounds.minRoomY - 2 || candidate.y > dungeonBounds.maxRoomY + 2
        
        withinWorld && farFromDungeon
      }
    }
    
    // Return first valid candidate, or fallback to corner
    allValidCandidates.headOption.getOrElse(Point(worldBounds.minRoomX + 1, worldBounds.minRoomY + 1))
  }
}
