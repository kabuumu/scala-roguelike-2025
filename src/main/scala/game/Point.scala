package game

case class Point(x: Int, y: Int) {
  def getDistance(otherPoint: Point): Double =
    Math.abs(x - otherPoint.x) + Math.abs(y - otherPoint.y)

  lazy val neighbors: Seq[Point] = Seq(
    Point(x + 1, y),
    Point(x - 1, y),
    Point(x, y + 1),
    Point(x, y - 1)
  )
}