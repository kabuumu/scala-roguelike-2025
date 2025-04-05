package dungeongenerator.generator

case class Point(x: Int, y: Int) {
  def distance(other: Point): Int = {
    val xDistance = Math.abs(x - other.x)
    val yDistance = Math.abs(y - other.y)

    xDistance + yDistance
  }

  lazy val adjacentPoints: Iterable[Point] = Set(
    Point(x + 1, y),
    Point(x - 1, y),
    Point(x, y + 1),
    Point(x, y - 1)
  )
}
