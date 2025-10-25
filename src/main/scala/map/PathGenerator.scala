package map

import game.Point
import scala.annotation.tailrec

/**
 * Generates dirt paths that lead towards dungeon entrances.
 * Paths help guide players to points of interest.
 */
object PathGenerator {
  
  /**
   * Generates a path from a start point to a target point.
   * The path is not perfectly straight but wanders slightly for a natural look.
   * 
   * @param startPoint Where the path begins (in tile coordinates)
   * @param targetPoint Where the path should lead (in tile coordinates)
   * @param width Width of the path in tiles
   * @param bounds Map bounds to constrain the path
   * @return Set of Points representing path tiles
   */
  def generatePath(
    startPoint: Point,
    targetPoint: Point,
    width: Int = 1,
    bounds: MapBounds
  ): Set[Point] = {
    val pathPoints = scala.collection.mutable.Set[Point]()
    
    // Generate main path line
    val mainPath = findPathLine(startPoint, targetPoint)
    
    // Add all points along the main path
    mainPath.foreach { point =>
      if (isWithinBounds(point, bounds)) {
        pathPoints += point
        
        // Add width to the path
        for {
          dx <- -width to width
          dy <- -width to width
          if (dx.abs + dy.abs) <= width // Manhattan distance for diamond shape
        } {
          val widthPoint = Point(point.x + dx, point.y + dy)
          if (isWithinBounds(widthPoint, bounds)) {
            pathPoints += widthPoint
          }
        }
      }
    }
    
    pathPoints.toSet
  }
  
  /**
   * Finds a line of points from start to target.
   * Uses Bresenham's line algorithm for a smooth path.
   */
  private def findPathLine(start: Point, target: Point): Seq[Point] = {
    val points = scala.collection.mutable.ArrayBuffer[Point]()
    
    val dx = math.abs(target.x - start.x)
    val dy = math.abs(target.y - start.y)
    val sx = if (start.x < target.x) 1 else -1
    val sy = if (start.y < target.y) 1 else -1
    var err = dx - dy
    
    var x = start.x
    var y = start.y
    
    while (x != target.x || y != target.y) {
      points += Point(x, y)
      
      val e2 = 2 * err
      if (e2 > -dy) {
        err -= dy
        x += sx
      }
      if (e2 < dx) {
        err += dx
        y += sy
      }
    }
    
    points += target
    points.toSeq
  }
  
  /**
   * Generates paths from multiple starting points to a single target.
   * Useful for creating paths that converge on a dungeon entrance.
   * 
   * @param startingPoints Multiple points from which paths should originate
   * @param targetPoint The dungeon entrance or point of interest
   * @param width Width of each path
   * @param bounds Map bounds
   * @return Set of all path Points
   */
  def generateConvergingPaths(
    startingPoints: Seq[Point],
    targetPoint: Point,
    width: Int = 1,
    bounds: MapBounds
  ): Set[Point] = {
    startingPoints.flatMap { start =>
      generatePath(start, targetPoint, width, bounds)
    }.toSet
  }
  
  /**
   * Generates paths that lead to all dungeon entrances in a world.
   * Creates paths from the world edges towards each dungeon.
   * 
   * @param dungeonEntrances Points where dungeons can be entered
   * @param bounds World bounds
   * @param pathsPerEntrance Number of paths leading to each entrance
   * @param seed Random seed for path start point selection
   * @return Set of all path Points
   */
  def generateDungeonPaths(
    dungeonEntrances: Seq[Point],
    bounds: MapBounds,
    pathsPerEntrance: Int = 2,
    width: Int = 1,
    seed: Long = System.currentTimeMillis()
  ): Set[Point] = {
    val random = new scala.util.Random(seed)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    dungeonEntrances.flatMap { entrance =>
      // For each entrance, create paths from random edge points
      (0 until pathsPerEntrance).flatMap { _ =>
        // Choose a random edge (0=top, 1=bottom, 2=left, 3=right)
        val edge = random.nextInt(4)
        val startPoint = edge match {
          case 0 => Point(random.between(tileMinX, tileMaxX + 1), tileMinY) // Top edge
          case 1 => Point(random.between(tileMinX, tileMaxX + 1), tileMaxY) // Bottom edge
          case 2 => Point(tileMinX, random.between(tileMinY, tileMaxY + 1)) // Left edge
          case 3 => Point(tileMaxX, random.between(tileMinY, tileMaxY + 1)) // Right edge
        }
        
        generatePath(startPoint, entrance, width, bounds)
      }
    }.toSet
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
   * Provides a human-readable description of generated paths.
   */
  def describePaths(paths: Set[Point], bounds: MapBounds): String = {
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    val totalArea = (tileMaxX - tileMinX + 1) * (tileMaxY - tileMinY + 1)
    val pathPercent = (paths.size.toDouble / totalArea * 100).toInt
    
    s"""Path Generation Summary:
       |  Total path tiles: ${paths.size}
       |  Coverage: $pathPercent% of map area
       |  Bounds: ${bounds.describe}""".stripMargin
  }
}
