package game

object LineOfSight {
  // Create method to return visible points from a point, using bresenham's line algorithm
  def getVisiblePoints(start: Point, blockedPoints: Set[Point], sightRange: Int): Set[Point] = {
    for(
      x <- start.x - sightRange to start.x + sightRange;
      y <- start.y - sightRange to start.y + sightRange;
      end = Point(x, y);
      line = getBresenhamLine(start, end)
      if start.getDistance(end) <= sightRange
      if line.drop(1).forall(!blockedPoints.contains(_))
    ) yield end
  }.toSet


  private def getBresenhamLine(start: Point, end: Point) = {
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
