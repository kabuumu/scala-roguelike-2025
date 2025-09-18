package game

enum Direction(val x: Int, val y: Int):
  case Up extends Direction(0, -1)
  case Down extends Direction(0, 1)
  case Left extends Direction(-1, 0)
  case Right extends Direction(1, 0)

object Direction {
  def oppositeOf(direction: Direction): Direction = direction match {
    case Direction.Up => Direction.Down
    case Direction.Down => Direction.Up
    case Direction.Left => Direction.Right
    case Direction.Right => Direction.Left
  }

  def asPoint(direction: Direction): Point = Point(direction.x, direction.y)

  def fromPoints(start: Point, end: Point): Direction = {
    val xDiff = end.x - start.x
    val yDiff = end.y - start.y

    if (Math.abs(xDiff) > Math.abs(yDiff)) {
      if (xDiff > 0) Direction.Right else Direction.Left
    } else {
      if (yDiff > 0) Direction.Down else Direction.Up
    }
  }
}