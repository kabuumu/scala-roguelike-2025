package map

import game.{Direction, Point}

case class Room(xSize: Int, ySize: Int, doors: Set[Direction]) {
  private def isDoor(tileLocation: Point): Boolean = doors.map {
    case Direction.Up => Point(xSize / 2, 0)
    case Direction.Down => Point(xSize / 2, ySize - 1)
    case Direction.Left => Point(0, ySize / 2)
    case Direction.Right => Point(xSize - 1, ySize / 2)
  }.contains(tileLocation)

  private def isWall(tileLocation: Point): Boolean =
    tileLocation.x == 0 || tileLocation.x == xSize - 1 || tileLocation.y == 0 || tileLocation.y == ySize - 1

  val tiles: GameMap = {
    val tiles = for {
      x <- 0 until xSize
      y <- 0 until ySize
    } yield Point(x, y) match {
      case point if isDoor(point) => (point, TileType.Floor)
      case point if isWall(point) => (point, TileType.Wall)
      case point => (point, TileType.Floor)
    }

    GameMap(tiles)
  }
}