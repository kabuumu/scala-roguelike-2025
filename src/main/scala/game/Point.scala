package game

case class Point(x: Int, y: Int) {
  def getChebyshevDistance(otherPoint: Point): Double =
    Math.max(Math.abs(x - otherPoint.x), Math.abs(y - otherPoint.y))
  
  def getSquaredEuclideanDistance(otherPoint: Point): Double = {
    val dx = x - otherPoint.x
    val dy = y - otherPoint.y
    dx * dx + dy * dy
  }

  // Check if a point is within a certain range of another point using Chebyshev distance
  def isWithinRangeOf(otherPoint: Point, range: Int): Boolean =
    getChebyshevDistance(otherPoint) <= range

  // Check if this point is within range of any point in the given set of points (e.g., entity hitbox)
  def isWithinRangeOfAny(otherPoints: Set[Point], range: Int): Boolean =
    otherPoints.exists(point => isWithinRangeOf(point, range))

  lazy val neighbors: Seq[Point] = Seq(
    Point(x + 1, y),
    Point(x - 1, y),
    Point(x, y + 1),
    Point(x, y - 1)
  )
  
  def +(direction: Direction): Point = Point(x + direction.x, y + direction.y)

  def +(other: Point): Point = Point(x + other.x, y + other.y)
  
  override def toString: String = s"$x:$y"
}

object Point {
  private val CacheMin = -256
  private val CacheMax = 256
  private val CacheSize = CacheMax - CacheMin + 1
  private val cache: Array[Point] = {
    val arr = new Array[Point](CacheSize * CacheSize)
    var x = CacheMin
    while (x <= CacheMax) {
      var y = CacheMin
      while (y <= CacheMax) {
        val idx = (x - CacheMin) * CacheSize + (y - CacheMin)
        arr(idx) = new Point(x, y)
        y += 1
      }
      x += 1
    }
    arr
  }

  def apply(x: Int, y: Int): Point = {
    if (x >= CacheMin && x <= CacheMax && y >= CacheMin && y <= CacheMax) {
      val idx = (x - CacheMin) * CacheSize + (y - CacheMin)
      cache(idx)
    } else {
      new Point(x, y)
    }
  }
}