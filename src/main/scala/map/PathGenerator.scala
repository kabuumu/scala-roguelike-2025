package map

import game.Point
import scala.annotation.tailrec

/**
 * Generates dirt paths that lead towards dungeon entrances.
 * Paths help guide players to points of interest.
 */
object PathGenerator {
  
  /**
   * Widens a path while avoiding obstacle points.
   */
  private def widenPathAvoidingObstacles(
    pathPoints: Seq[Point],
    width: Int,
    bounds: MapBounds,
    obstacles: Set[Point]
  ): Set[Point] = {
    val result = scala.collection.mutable.Set[Point]()
    
    pathPoints.foreach { point =>
      if (isWithinBounds(point, bounds)) {
        result += point
        
        // Add width to the path, avoiding obstacles
        for {
          dx <- -width to width
          dy <- -width to width
          if (dx.abs + dy.abs) <= width
        } {
          val widthPoint = Point(point.x + dx, point.y + dy)
          if (isWithinBounds(widthPoint, bounds) && !obstacles.contains(widthPoint)) {
            result += widthPoint
          }
        }
      }
    }
    
    result.toSet
  }
  
  /**
   * Generates a path from a start point to a target point, avoiding dungeon obstacles.
   * Uses A* pathfinding to navigate around dungeon walls and rooms.
   * The path will only connect to the entrance room, not cut through other dungeon areas.
   * 
   * @param startPoint Where the path begins (in tile coordinates)
   * @param targetPoint Where the path should lead (in tile coordinates)
   * @param obstacles Points that must be avoided (dungeon walls, etc.)
   * @param width Width of the path in tiles
   * @param bounds Map bounds to constrain the path
   * @return Set of Points representing path tiles
   */
  def generatePathAroundObstacles(
    startPoint: Point,
    targetPoint: Point,
    obstacles: Set[Point],
    width: Int,
    bounds: MapBounds,
    existingPaths: Set[Point] = Set.empty
  ): Set[Point] = {
    // Dynamically remove start and target from obstacles to ensure pathfinder endpoints are accessible
    val safeObstacles = obstacles - startPoint - targetPoint
    val mainPath = findPathAroundObstacles(startPoint, targetPoint, safeObstacles, bounds, existingPaths)
    
    // If pathfinding failed, fall back to direct line
    val finalPath = if (mainPath.isEmpty) {
      findPathLine(startPoint, targetPoint)
    } else {
      mainPath
    }
    
    // Add all points along the path with width, excluding obstacles
    widenPathAvoidingObstacles(finalPath, width, bounds, safeObstacles)
  }
  
  /**
   * Finds a path avoiding obstacles using A* pathfinding.
   * Returns empty sequence if no path can be found.
   * Prefers straight paths and reuses existing path tiles when available.
   */
  private def findPathAroundObstacles(
    start: Point, 
    target: Point, 
    obstacles: Set[Point],
    bounds: MapBounds,
    existingPaths: Set[Point] = Set.empty
  ): Seq[Point] = {
    import scala.collection.mutable
    
    val safeObstacles = obstacles - start - target

    if (!isWithinBounds(start, bounds) || !isWithinBounds(target, bounds)) {
      return Seq.empty
    }
    
    case class Node(point: Point, g: Double, h: Double, parent: Option[Node], direction: Option[(Int, Int)]) {
      val f: Double = g + h
    }
    
    def heuristic(a: Point, b: Point): Double = 
      math.abs(a.x - b.x) + math.abs(a.y - b.y)
    
    def getDirection(from: Point, to: Point): (Int, Int) = {
      val dx = if (to.x > from.x) 1 else if (to.x < from.x) -1 else 0
      val dy = if (to.y > from.y) 1 else if (to.y < from.y) -1 else 0
      (dx, dy)
    }
    
    def reconstructPath(node: Node): Seq[Point] = {
      @tailrec
      def loop(n: Node, acc: List[Point]): Seq[Point] = n.parent match {
        case Some(parent) => loop(parent, n.point :: acc)
        case None => n.point :: acc
      }
      loop(node, Nil)
    }
    
    implicit val nodeOrdering: Ordering[Node] = Ordering.by[Node, Double](-_.f)
    val openSet = mutable.PriorityQueue(Node(start, 0.0, heuristic(start, target), None, None))
    val closedSet = mutable.HashSet[Point]()
    val gScores = mutable.HashMap[Point, Double](start -> 0.0)
    
    while (openSet.nonEmpty) {
      val current = openSet.dequeue()
      
      if (current.point == target) {
        return reconstructPath(current)
      }
      
      if (!closedSet.contains(current.point)) {
        closedSet += current.point
        
        // Get neighbors (4-directional movement)
        val neighbors = Seq(
          Point(current.point.x + 1, current.point.y),
          Point(current.point.x - 1, current.point.y),
          Point(current.point.x, current.point.y + 1),
          Point(current.point.x, current.point.y - 1)
        ).filter { neighbor =>
          isWithinBounds(neighbor, bounds) && !safeObstacles.contains(neighbor)
        }
        
        neighbors.foreach { neighbor =>
          val neighborDirection = getDirection(current.point, neighbor)
          
          // Cost discount for existing path tiles (0.15 vs 1.0) to encourage merging
          var stepCost = if (existingPaths.contains(neighbor)) 0.15 else 1.0
          
          // Add small penalty for changing direction (prefer straight lines)
          current.direction match {
            case Some(prevDir) if prevDir != neighborDirection =>
              stepCost += 0.1
            case _ =>
          }
          
          val tentativeG = current.g + stepCost
          
          if (tentativeG < gScores.getOrElse(neighbor, Double.MaxValue)) {
            gScores(neighbor) = tentativeG
            val h = heuristic(neighbor, target)
            openSet.enqueue(Node(neighbor, tentativeG, h, Some(current), Some(neighborDirection)))
          }
        }
      }
    }
    
    Seq.empty
  }
  
  /**
   * Finds a line of points from start to target using orthogonal (4-directional) movement.
   * Creates an L-shaped path: moves horizontally first, then vertically.
   * This ensures no diagonal movement, only straight lines.
   */
  private def findPathLine(start: Point, target: Point): Seq[Point] = {
    val points = scala.collection.mutable.ArrayBuffer[Point]()
    
    // Move horizontally first
    val xStep = if (start.x < target.x) 1 else if (start.x > target.x) -1 else 0
    var x = start.x
    while (x != target.x) {
      points += Point(x, start.y)
      x += xStep
    }
    
    // Then move vertically
    val yStep = if (start.y < target.y) 1 else if (start.y > target.y) -1 else 0
    var y = start.y
    while (y != target.y) {
      points += Point(target.x, y)
      y += yStep
    }
    
    // Add the final target point
    points += target
    points.toSeq
  }
  
  /**
   * Checks if a point is within the specified bounds.
   */
  private def isWithinBounds(point: Point, bounds: MapBounds): Boolean = {
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    point.x >= tileMinX && point.x <= tileMaxX &&
    point.y >= tileMinY && point.y <= tileMaxY
  }
}
