package map

import game.Point
import scala.util.Random

/**
 * Generates rivers that flow across the map with natural curves.
 * Rivers are non-straight and add visual interest to the world.
 */
object RiverGenerator {
  
  /**
   * Generates a river that flows across the map with natural curves.
   * 
   * @param config RiverConfig specifying start, direction, and curviness
   * @return Set of Points representing river tiles
   */
  def generateRiver(config: RiverConfig): Set[Point] = {
    val random = new Random(config.seed)
    
    // Start with the initial point
    var currentPoint = config.startPoint
    val riverPoints = scala.collection.mutable.Set[Point](currentPoint)
    
    // Current direction (we'll vary this for curves)
    var currentDirection = config.flowDirection
    
    // Generate river segments
    var i = 0
    var withinBounds = true
    while (i < config.length && withinBounds) {
      // Occasionally change direction for curves (non-straight lines)
      if (random.nextDouble() < config.curviness) {
        currentDirection = perturbDirection(currentDirection, random)
      }
      
      // Move in current direction
      currentPoint = moveInDirection(currentPoint, currentDirection)
      
      // Add the river point
      if (isWithinBounds(currentPoint, config.bounds)) {
        riverPoints += currentPoint
        
        // Add width to the river
        for (j <- 1 to config.width) {
          val perpendicular1 = getPerpendicularOffset(currentDirection, j)
          val perpendicular2 = getPerpendicularOffset(currentDirection, -j)
          
          val widthPoint1 = Point(currentPoint.x + perpendicular1._1, currentPoint.y + perpendicular1._2)
          val widthPoint2 = Point(currentPoint.x + perpendicular2._1, currentPoint.y + perpendicular2._2)
          
          if (isWithinBounds(widthPoint1, config.bounds)) {
            riverPoints += widthPoint1
          }
          if (isWithinBounds(widthPoint2, config.bounds)) {
            riverPoints += widthPoint2
          }
        }
        i += 1
      } else {
        // River has left the bounds, stop generating
        withinBounds = false
      }
    }
    
    riverPoints.toSet
  }
  
  /**
   * Perturbs a direction slightly for natural curves.
   * Changes the direction by rotating it slightly.
   */
  private def perturbDirection(direction: (Int, Int), random: Random): (Int, Int) = {
    // Choose to rotate left or right
    if (random.nextBoolean()) {
      rotateLeft(direction)
    } else {
      rotateRight(direction)
    }
  }
  
  /**
   * Rotates a direction vector 45 degrees left (counter-clockwise).
   */
  private def rotateLeft(direction: (Int, Int)): (Int, Int) = direction match {
    case (0, -1) => (-1, -1)  // Up -> UpLeft
    case (-1, -1) => (-1, 0)  // UpLeft -> Left
    case (-1, 0) => (-1, 1)   // Left -> DownLeft
    case (-1, 1) => (0, 1)    // DownLeft -> Down
    case (0, 1) => (1, 1)     // Down -> DownRight
    case (1, 1) => (1, 0)     // DownRight -> Right
    case (1, 0) => (1, -1)    // Right -> UpRight
    case (1, -1) => (0, -1)   // UpRight -> Up
    case _ => direction       // Default: no change
  }
  
  /**
   * Rotates a direction vector 45 degrees right (clockwise).
   */
  private def rotateRight(direction: (Int, Int)): (Int, Int) = direction match {
    case (0, -1) => (1, -1)   // Up -> UpRight
    case (1, -1) => (1, 0)    // UpRight -> Right
    case (1, 0) => (1, 1)     // Right -> DownRight
    case (1, 1) => (0, 1)     // DownRight -> Down
    case (0, 1) => (-1, 1)    // Down -> DownLeft
    case (-1, 1) => (-1, 0)   // DownLeft -> Left
    case (-1, 0) => (-1, -1)  // Left -> UpLeft
    case (-1, -1) => (0, -1)  // UpLeft -> Up
    case _ => direction       // Default: no change
  }
  
  /**
   * Moves a point in the given direction.
   */
  private def moveInDirection(point: Point, direction: (Int, Int)): Point = {
    Point(point.x + direction._1, point.y + direction._2)
  }
  
  /**
   * Gets a perpendicular offset to the given direction.
   * Used for adding width to rivers.
   */
  private def getPerpendicularOffset(direction: (Int, Int), distance: Int): (Int, Int) = {
    direction match {
      case (0, -1) | (0, 1) => (distance, 0)  // Vertical flow -> horizontal offset
      case (1, 0) | (-1, 0) => (0, distance)  // Horizontal flow -> vertical offset
      case (1, -1) | (-1, 1) => (distance, distance)  // Diagonal NE-SW -> perpendicular
      case (1, 1) | (-1, -1) => (distance, -distance) // Diagonal NW-SE -> perpendicular
      case _ => (distance, 0)
    }
  }
  
  /**
   * Checks if a point is within the specified bounds.
   */
  private def isWithinBounds(point: Point, bounds: MapBounds): Boolean = {
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    point.x >= tileMinX && point.x <= tileMaxX &&
    point.y >= tileMinY && point.y <= tileMaxY
  }
  
  /**
   * Generates multiple rivers across a map.
   * 
   * @param configs Sequence of RiverConfig for each river
   * @return Set of all river Points
   */
  def generateRivers(configs: Seq[RiverConfig]): Set[Point] = {
    configs.flatMap(generateRiver).toSet
  }
  
  /**
   * Provides a human-readable description of generated rivers.
   */
  def describeRivers(rivers: Set[Point], bounds: MapBounds): String = {
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    val totalArea = (tileMaxX - tileMinX + 1) * (tileMaxY - tileMinY + 1)
    val riverPercent = (rivers.size.toDouble / totalArea * 100).toInt
    
    s"""River Generation Summary:
       |  Total river tiles: ${rivers.size}
       |  Coverage: $riverPercent% of map area
       |  Bounds: ${bounds.describe}""".stripMargin
  }
}

/**
 * Configuration for river generation.
 * 
 * @param startPoint Starting point for the river (in tile coordinates)
 * @param flowDirection Initial direction vector (dx, dy)
 * @param length How many steps the river should flow
 * @param width Width of the river (0 = single tile wide, 1 = 3 tiles wide, etc.)
 * @param curviness Probability (0.0 to 1.0) of changing direction at each step
 * @param bounds Map bounds to constrain the river
 * @param seed Random seed for reproducible generation
 */
case class RiverConfig(
  startPoint: Point,
  flowDirection: (Int, Int) = (0, 1),  // Default: flows down
  length: Int = 50,
  width: Int = 1,
  curviness: Double = 0.2,
  bounds: MapBounds,
  seed: Long = System.currentTimeMillis()
) {
  require(curviness >= 0.0 && curviness <= 1.0, "curviness must be between 0.0 and 1.0")
  require(length > 0, "length must be positive")
  require(width >= 0, "width must be non-negative")
}
