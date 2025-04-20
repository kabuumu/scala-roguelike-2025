package game

case class Point(x: Int, y: Int) {
  def getChebyshevDistance(otherPoint: Point): Double =
    Math.max(Math.abs(x - otherPoint.x), Math.abs(y - otherPoint.y))
  
  def getSquaredEuclideanDistance(otherPoint: Point): Double =
    Math.pow(x - otherPoint.x, 2) + Math.pow(y - otherPoint.y, 2)

  // Check if a point is within a certain range of another point using Chebyshev distance
  def isWithinRangeOf(otherPoint: Point, range: Int): Boolean =
    getChebyshevDistance(otherPoint) <= range

  lazy val neighbors: Seq[Point] = Seq(
    Point(x + 1, y),
    Point(x - 1, y),
    Point(x, y + 1),
    Point(x, y - 1)
  )
  
  def +(direction: Direction): Point = Point(x + direction.x, y + direction.y)
  
  override def toString: String = s"$x:$y"
}