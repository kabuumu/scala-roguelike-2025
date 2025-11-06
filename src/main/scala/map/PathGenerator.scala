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
    bounds: MapBounds
  ): Set[Point] = {
    // Use A* pathfinding to find route around obstacles
    val mainPath = findPathAroundObstacles(startPoint, targetPoint, obstacles, bounds)
    
    // If pathfinding failed, fall back to direct line
    val finalPath = if (mainPath.isEmpty) {
      println(s"Falling back to direct line from $startPoint to $targetPoint")
      findPathLine(startPoint, targetPoint)
    } else {
      mainPath
    }
    
    // Add all points along the path with width, excluding obstacles
    widenPathAvoidingObstacles(finalPath, width, bounds, obstacles)
  }
  
  /**
   * Finds a path avoiding obstacles using A* pathfinding.
   * Returns empty sequence if no path can be found.
   * Prefers straight paths and minimizes corners.
   */
  private def findPathAroundObstacles(
    start: Point, 
    target: Point, 
    obstacles: Set[Point],
    bounds: MapBounds
  ): Seq[Point] = {
    import scala.collection.mutable
    
    // Check if start or target is an obstacle or out of bounds
    if (obstacles.contains(start)) {
      println(s"A* pathfinding failed: Start point $start is an obstacle")
      return Seq.empty
    }
    if (obstacles.contains(target)) {
      println(s"A* pathfinding failed: Target point $target is an obstacle")
      return Seq.empty
    }
    if (!isWithinBounds(start, bounds)) {
      println(s"A* pathfinding failed: Start point $start is outside bounds")
      return Seq.empty
    }
    if (!isWithinBounds(target, bounds)) {
      println(s"A* pathfinding failed: Target point $target is outside bounds")
      return Seq.empty
    }
    
    case class Node(point: Point, g: Int, h: Int, parent: Option[Node], direction: Option[(Int, Int)]) {
      val f: Int = g + h
    }
    
    def heuristic(a: Point, b: Point): Int = 
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
    
    implicit val nodeOrdering: Ordering[Node] = Ordering.by[Node, Int](-_.f)
    val openSet = mutable.PriorityQueue(Node(start, 0, heuristic(start, target), None, None))
    val closedSet = mutable.HashSet[Point]()
    val gScores = mutable.HashMap[Point, Int](start -> 0)
    
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
          isWithinBounds(neighbor, bounds) && !obstacles.contains(neighbor)
        }
        
        neighbors.foreach { neighbor =>
          val neighborDirection = getDirection(current.point, neighbor)
          
          // Base cost is 1 for movement
          var movementCost = 1
          
          // Add a small penalty for changing direction (prefer straight lines)
          // This helps minimize corners in the path
          current.direction match {
            case Some(prevDir) if prevDir != neighborDirection =>
              // Direction change: add small penalty (0.1 * 10 = 1 as integer)
              movementCost += 1
            case _ =>
              // Same direction or first move: no penalty
          }
          
          val tentativeG = current.g + movementCost
          
          if (tentativeG < gScores.getOrElse(neighbor, Int.MaxValue)) {
            gScores(neighbor) = tentativeG
            val h = heuristic(neighbor, target)
            openSet.enqueue(Node(neighbor, tentativeG, h, Some(current), Some(neighborDirection)))
          }
        }
      }
    }
    
    // No path found - all options exhausted
    println(s"A* pathfinding failed: No path found from $start to $target (checked ${closedSet.size} tiles)")
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
