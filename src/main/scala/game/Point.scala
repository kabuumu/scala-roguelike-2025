package game

case class Point(x: Int, y: Int) {
  def getManhattanDistance(otherPoint: Point): Double =
    Math.abs(x - otherPoint.x) + Math.abs(y - otherPoint.y)

  def getEuclideanDistance(otherPoint: Point): Double =
    Math.sqrt(Math.pow(x - otherPoint.x, 2) + Math.pow(y - otherPoint.y, 2))

  def getChebyshevDistance(otherPoint: Point): Double =
    Math.max(Math.abs(x - otherPoint.x), Math.abs(y - otherPoint.y))

  def getMinkowskiDistance(otherPoint: Point, p: Double): Double =
    Math.pow(Math.pow(Math.abs(x - otherPoint.x), p) + Math.pow(Math.abs(y - otherPoint.y), p), 1 / p)

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
}