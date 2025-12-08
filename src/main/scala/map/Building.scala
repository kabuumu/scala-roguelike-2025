package map

import game.Point
import map.TileType._

enum BuildingType:
  case Healer
  case PotionShop
  case EquipmentShop
  case Generic

/**
 * Represents a single building in a village.
 * Buildings are rectangular structures with configurable dimensions.
 * 
 * @param location The room coordinate where the building is located
 * @param width Width of the building in tiles (internal space)
 * @param height Height of the building in tiles (internal space)
 * @param buildingType The type of building (Healer, Shop, etc.)
 */
case class Building(
  location: Point,
  width: Int,
  height: Int,
  buildingType: BuildingType = BuildingType.Generic
) {
  require(width >= 5, "Building width must be at least 5")
  require(height >= 5, "Building height must be at least 5")
  
  /**
   * Helper for backward compatibility or general shop check
   */
  def isShop: Boolean = buildingType != BuildingType.Generic

  /**
   * Calculate the center tile position of the building.
   */
  def centerTile: Point = {
    Point(
      location.x + width / 2,
      location.y + height / 2
    )
  }
  
  /**
   * Calculate the entrance tile position (middle of one wall).
   * The entrance faces the origin (0, 0) for easier access.
   */
  def entranceTile: Point = {
    val baseX = location.x
    val baseY = location.y
    
    // Determine which side to place entrance based on location relative to origin
    if (location.x >= 0 && location.y >= 0) {
      // Building is in quadrant I (bottom-right) - entrance on left or top
      if (location.x > location.y) {
        // More to the right, entrance on left
        Point(baseX, baseY + height / 2)
      } else {
        // More up, entrance on top
        Point(baseX + width / 2, baseY)
      }
    } else if (location.x < 0 && location.y >= 0) {
      // Building is in quadrant II (bottom-left) - entrance on right or top
      if (-location.x > location.y) {
        // More to the left, entrance on right
        Point(baseX + width, baseY + height / 2)
      } else {
        // More up, entrance on top
        Point(baseX + width / 2, baseY)
      }
    } else if (location.x < 0 && location.y < 0) {
      // Building is in quadrant III (top-left) - entrance on right or bottom
      if (-location.x > -location.y) {
        // More to the left, entrance on right
        Point(baseX + width, baseY + height / 2)
      } else {
        // More up, entrance on bottom
        Point(baseX + width / 2, baseY + height)
      }
    } else {
      // Building is in quadrant IV (top-right) - entrance on left or bottom
      if (location.x > -location.y) {
        // More to the right, entrance on left
        Point(baseX, baseY + height / 2)
      } else {
        // More down, entrance on bottom
        Point(baseX + width / 2, baseY + height)
      }
    }
  }
  
  /**
   * Generate all tiles for the building.
   * Creates a rectangular building with brick walls and floor inside.
   */
  lazy val tiles: Map[Point, TileType] = {
    val baseX = location.x
    val baseY = location.y
    val entrance = entranceTile
    
    val buildingTiles = for {
      x <- baseX to (baseX + width)
      y <- baseY to (baseY + height)
    } yield {
      val point = Point(x, y)
      
      // Determine if this is a wall position
      val isWall = x == baseX || x == baseX + width || y == baseY || y == baseY + height
      
      // Is this the entrance?
      val isEntrance = point == entrance
      
      val tileType = if (isWall && !isEntrance) {
        TileType.Wall  // Brick walls
      } else {
        TileType.Floor  // Floor inside
      }
      
      point -> tileType
    }
    
    buildingTiles.toMap
  }
  
  /**
   * All wall points in the building.
   */
  lazy val walls: Set[Point] = tiles.filter(_._2 == TileType.Wall).keySet
  
  /**
   * Bounds of this building in tile coordinates.
   */
  def bounds: (Point, Point) = (location, Point(location.x + width, location.y + height))
}

object Building {
  /**
   * Check if two buildings overlap.
   */
  def overlap(b1: Building, b2: Building): Boolean = {
    val (b1Min, b1Max) = b1.bounds
    val (b2Min, b2Max) = b2.bounds
    
    !(b1Max.x < b2Min.x || b2Max.x < b1Min.x || b1Max.y < b2Min.y || b2Max.y < b1Min.y)
  }
}
