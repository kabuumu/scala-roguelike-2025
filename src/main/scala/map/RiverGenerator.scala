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
   * Rivers follow a step-based pattern: move varianceStep tiles, then randomly
   * change width and direction.
   * 
   * @param config RiverConfig specifying start, direction, and variance parameters
   * @return Set of Points representing river tiles
   */
  def generateRiver(config: RiverConfig): Set[Point] = {
    val random = new Random(config.seed)
    
    // Start with the initial point
    var currentPoint = config.startPoint
    val riverPoints = scala.collection.mutable.Set[Point](currentPoint)
    
    // Current direction and width (we'll vary these)
    val originalDirection = config.flowDirection  // Track original direction
    var currentDirection = config.flowDirection
    var currentWidth = config.width
    
    // Generate river segments
    var stepsSinceLastChange = 0
    var totalSteps = 0
    var withinBounds = true
    
    while (totalSteps < config.length && withinBounds) {
      // Move in current direction
      currentPoint = moveInDirection(currentPoint, currentDirection)
      
      // Add the river point
      if (isWithinBounds(currentPoint, config.bounds)) {
        riverPoints += currentPoint
        
        // Add all tiles within Manhattan distance (width) from the center point
        for (dx <- -currentWidth to currentWidth) {
          for (dy <- -currentWidth to currentWidth) {
            val manhattanDist = math.abs(dx) + math.abs(dy)
            if (manhattanDist <= currentWidth && manhattanDist > 0) {
              val widthPoint = Point(currentPoint.x + dx, currentPoint.y + dy)
              if (isWithinBounds(widthPoint, config.bounds)) {
                riverPoints += widthPoint
              }
            }
          }
        }
        
        stepsSinceLastChange += 1
        totalSteps += 1
        
        // After varianceStep tiles, randomly change width and direction
        if (stepsSinceLastChange >= config.varianceStep) {
          // Randomly change width by ±1 (within bounds)
          if (random.nextDouble() < config.widthVariance) {
            val widthChange = if (random.nextBoolean()) 1 else -1
            currentWidth = math.max(0, math.min(5, currentWidth + widthChange))
          }
          
          // Direction changes are less likely the farther we are from original direction
          // Calculate angular difference (0 = same direction, higher = more different)
          val angleDiff = calculateAngleDifference(originalDirection, currentDirection)
          
          // Reduce curve probability based on how far we've deviated
          // angleDiff ranges from 0 (same) to 4 (opposite), so we scale down the variance
          val adjustedCurveVariance = config.curveVariance * (1.0 - (angleDiff / 8.0))
          
          if (random.nextDouble() < adjustedCurveVariance) {
            // Normal perturbation (rotate left or right)
            currentDirection = perturbDirection(currentDirection, random)
          } else if (angleDiff > 0 && random.nextDouble() < 0.3) {
            // Pull back toward original direction
            currentDirection = pullTowardDirection(currentDirection, originalDirection)
          }
          
          stepsSinceLastChange = 0
        }
      } else {
        // River has left the bounds, stop generating
        withinBounds = false
      }
    }
    
    riverPoints.toSet
  }
  
  /**
   * Calculates the angular difference between two directions.
   * Returns 0 if same direction, higher values for greater differences.
   */
  private def calculateAngleDifference(dir1: (Int, Int), dir2: (Int, Int)): Int = {
    if (dir1 == dir2) return 0
    
    // Count rotations needed to get from dir1 to dir2
    var current = dir1
    var rotations = 0
    
    // Try rotating left
    var leftRotations = 0
    current = dir1
    while (current != dir2 && leftRotations < 8) {
      current = rotateLeft(current)
      leftRotations += 1
    }
    
    // Try rotating right
    var rightRotations = 0
    current = dir1
    while (current != dir2 && rightRotations < 8) {
      current = rotateRight(current)
      rightRotations += 1
    }
    
    // Return the minimum rotations needed
    math.min(leftRotations, rightRotations)
  }
  
  /**
   * Pulls the current direction one step closer to the target direction.
   * Chooses the shorter rotation path (left or right).
   */
  private def pullTowardDirection(current: (Int, Int), target: (Int, Int)): (Int, Int) = {
    if (current == target) return current
    
    // Check if rotating left gets us closer
    val rotatedLeft = rotateLeft(current)
    val rotatedRight = rotateRight(current)
    
    val leftDiff = calculateAngleDifference(rotatedLeft, target)
    val rightDiff = calculateAngleDifference(rotatedRight, target)
    
    // Rotate in the direction that gets us closer
    if (leftDiff < rightDiff) rotatedLeft else rotatedRight
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
   * Generates a river starting from a world map edge, facing toward the center.
   * 
   * @param bounds World map bounds
   * @param edge Which edge to start from (0=top, 1=bottom, 2=left, 3=right)
   * @param initialWidth Initial river width (1-5)
   * @param widthVariance Probability of width changing
   * @param curveVariance Probability of direction changing
   * @param varianceStep Number of tiles between variance changes
   * @param seed Random seed
   * @return RiverConfig for generating the river
   */
  def createEdgeRiver(
    bounds: MapBounds,
    edge: Int,
    initialWidth: Int,
    widthVariance: Double,
    curveVariance: Double,
    varianceStep: Int,
    seed: Long
  ): RiverConfig = {
    val random = new Random(seed)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    val centerX = (tileMinX + tileMaxX) / 2
    val centerY = (tileMinY + tileMaxY) / 2
    
    // Calculate start point and direction based on edge
    val (startPoint, flowDirection) = edge match {
      case 0 => // Top edge - flow downward (toward center)
        val x = random.between(tileMinX + 10, tileMaxX - 10)
        (Point(x, tileMinY), (0, 1))
      case 1 => // Bottom edge - flow upward (toward center)
        val x = random.between(tileMinX + 10, tileMaxX - 10)
        (Point(x, tileMaxY), (0, -1))
      case 2 => // Left edge - flow rightward (toward center)
        val y = random.between(tileMinY + 10, tileMaxY - 10)
        (Point(tileMinX, y), (1, 0))
      case 3 => // Right edge - flow leftward (toward center)
        val y = random.between(tileMinY + 10, tileMaxY - 10)
        (Point(tileMaxX, y), (-1, 0))
      case _ => // Default to top edge
        (Point(centerX, tileMinY), (0, 1))
    }
    
    // Calculate length: river should be able to cross most of the map
    val maxDimension = math.max(tileMaxX - tileMinX, tileMaxY - tileMinY)
    val length = (maxDimension * 1.2).toInt
    
    RiverConfig(
      startPoint = startPoint,
      flowDirection = flowDirection,
      length = length,
      width = initialWidth,
      widthVariance = widthVariance,
      curveVariance = curveVariance,
      varianceStep = varianceStep,
      bounds = bounds,
      seed = seed
    )
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
 * @param width Initial width of the river (0 = single tile wide, 1 = 3 tiles wide, etc.)
 * @param widthVariance Probability (0.0 to 1.0) of changing width at each variance step
 * @param curveVariance Probability (0.0 to 1.0) of changing direction at each variance step
 * @param varianceStep Number of tiles to move before applying variance changes
 * @param bounds Map bounds to constrain the river
 * @param seed Random seed for reproducible generation
 * @param curviness Deprecated - use curveVariance instead (maintained for backward compatibility)
 */
case class RiverConfig(
  startPoint: Point,
  flowDirection: (Int, Int),  // Default: flows down
  length: Int,
  width: Int,
  widthVariance: Double,
  curveVariance: Double,
  varianceStep: Int,
  bounds: MapBounds,
  seed: Long,
) {
  require(widthVariance >= 0.0 && widthVariance <= 1.0, "widthVariance must be between 0.0 and 1.0")
  require(curveVariance >= 0.0 && curveVariance <= 1.0, "curveVariance must be between 0.0 and 1.0")
  require(length > 0, "length must be positive")
  require(width >= 0 && width <= 5, "width must be between 0 and 5")
  require(varianceStep > 0, "varianceStep must be positive")
}
