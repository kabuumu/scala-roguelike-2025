package game

case class Point(x: Int, y: Int) {
  def getDistance(otherPoint: Point): Double =
    Math.abs(x - otherPoint.x) + Math.abs(y - otherPoint.y)
}