package scala.util

import game.Point

object LineOfSight {
  // Create method to return visible points from a point, using bresenham's line algorithm
  // Optimized: Check range BEFORE calculating line, and use early termination on blockers
  def getVisiblePoints(start: Point, blockedPoints: Set[Point], sightRange: Int): Set[Point] = {
    val result = Set.newBuilder[Point]
    
    // Iterate over the sight range square
    var x = start.x - sightRange
    while (x <= start.x + sightRange) {
      var y = start.y - sightRange
      while (y <= start.y + sightRange) {
        val end = Point(x, y)
        // Check range BEFORE computing expensive Bresenham line
        if (start.isWithinRangeOf(end, sightRange)) {
          // Use early-termination line check instead of computing full line
          if (isVisible(start, end, blockedPoints)) {
            result += end
          }
        }
        y += 1
      }
      x += 1
    }
    
    result.result()
  }
  
  // Optimized visibility check with early termination
  // Returns true if line of sight is not blocked between start and end
  private def isVisible(start: Point, end: Point, blockedPoints: Set[Point]): Boolean = {
    val dx = Math.abs(end.x - start.x)
    val dy = Math.abs(end.y - start.y)
    val sx = if (start.x < end.x) 1 else -1
    val sy = if (start.y < end.y) 1 else -1
    var x = start.x
    var y = start.y
    var err = dx - dy
    
    // Skip the start point, check intermediate points
    while (x != end.x || y != end.y) {
      val e2 = 2 * err
      if (e2 > -dy) {
        err -= dy
        x += sx
      }
      if (e2 < dx) {
        err += dx
        y += sy
      }
      // Early termination: if we hit a blocker before reaching end, return false
      // But allow seeing the end point if it's the only blocker
      if ((x != end.x || y != end.y) && blockedPoints.contains(Point(x, y))) {
        return false
      }
    }
    true
  }

  def getBresenhamLine(start: Point, end: Point): Seq[Point] = {
    val dx = Math.abs(end.x - start.x)
    val dy = Math.abs(end.y - start.y)
    val sx = if (start.x < end.x) 1 else -1
    val sy = if (start.y < end.y) 1 else -1
    var (x, y) = (start.x, start.y)
    var err = dx - dy
    var points = List(Point(x, y))
    while (x != end.x || y != end.y) {
      val e2 = 2 * err
      if (e2 > -dy) {
        err -= dy
        x += sx
      }
      if (e2 < dx) {
        err += dx
        y += sy
      }
      points = Point(x, y) :: points
    }
    points
  }
}
