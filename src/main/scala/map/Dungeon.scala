package map

import game.{Direction, Point}

case class Dungeon(roomGrid: Set[Point] = Set(Point(0, 0)), roomConnections: Set[RoomConnection] = Set.empty) {
  def addRoom(originRoom: Point, direction: Direction): Dungeon = {
    val newRoom = originRoom + direction

    if (roomGrid.contains(newRoom)) {
      throw new IllegalArgumentException(s"Room already exists at $newRoom")
    }

    copy(roomGrid = roomGrid + newRoom)
      .addConnection(originRoom, newRoom, direction)
  }

  def addConnection(from: Point, to: Point, direction: Direction): Dungeon = {
    copy(roomConnections =
      roomConnections + RoomConnection(from, direction, to)
        + RoomConnection(to, Direction.oppositeOf(direction), from)
    )
  }

  val availableRooms: Set[(Point, Direction)] =
    roomGrid.flatMap { room =>
      Direction.values.map { direction =>
        (room, direction)
      }
    }.filterNot { case (room, direction) =>
      roomGrid.contains(room + direction)
    }

  val doorPoints: Set[Point] = roomConnections.map {
    case RoomConnection(originRoom, direction, _) =>
      val originRoomX = originRoom.x * Dungeon.roomSize
      val originRoomY = originRoom.y * Dungeon.roomSize

      direction match {
        case Direction.Up => Point(originRoomX + Dungeon.roomSize / 2, originRoomY)
        case Direction.Down => Point(originRoomX + Dungeon.roomSize / 2, originRoomY + Dungeon.roomSize - 1)
        case Direction.Left => Point(originRoomX, originRoomY + Dungeon.roomSize / 2)
        case Direction.Right => Point(originRoomX + Dungeon.roomSize - 1, originRoomY + Dungeon.roomSize / 2)
      }
  }

  val tiles: Map[Point, TileType] = roomGrid.flatMap {
    room =>
      val roomX = room.x * Dungeon.roomSize
      val roomY = room.y * Dungeon.roomSize

      def isWall(point: Point) = point.x == roomX || point.x == roomX + Dungeon.roomSize - 1 || point.y == roomY || point.y == roomY + Dungeon.roomSize - 1

      def isDoor(point: Point) = doorPoints.contains(point)

      val roomTiles = for {
        x <- roomX until roomX + Dungeon.roomSize
        y <- roomY until roomY + Dungeon.roomSize
      } yield {
        val point = Point(x, y)
        if (isDoor(point)) {
          (point, TileType.Floor)
        } else if (isWall(point)) {
          (point, TileType.Wall)
        } else {
          (point, TileType.Floor)
        }
      }

      roomTiles.toMap
  }.toMap
}


case class RoomConnection(originRoom: Point, direction: Direction, destinationRoom: Point)

object Dungeon {
  val roomSize = 11
}