package map

import dungeongenerator.generator.Entity.KeyColour.Red
import game.EntityType.LockedDoor
import game.Item.Item
import game.{Direction, Point}
import map.TileType.Wall

case class Dungeon(roomGrid: Set[Point] = Set(Point(0, 0)),
                   roomConnections: Set[RoomConnection] = Set.empty,
                   blockedRooms: Set[Point] = Set.empty, //Rooms to no longer add connections to
                   startPoint: Point = Point(0, 0),
                   endpoint: Option[Point] = None,
                   items: Set[(Point, Item)] = Set.empty) {
  def lockRoomConnection(roomConnection: RoomConnection, lock: LockedDoor): Dungeon = {
    copy(
      roomConnections = roomConnections - roomConnection + roomConnection.copy(
        optLock = Some(lock))
    )
  }

  def addItem(room: Point, item: Item): Dungeon = {
    if (roomGrid.contains(room)) {
      copy(items = items + (room -> item))
    } else {
      throw new IllegalArgumentException(s"Room $room does not exist in the dungeon")
    }
  }

  def blockRoom(room: Point): Dungeon = {
    if (roomGrid.contains(room)) {
      copy(blockedRooms = blockedRooms + room)
    } else {
      throw new IllegalArgumentException(s"Room $room does not exist in the dungeon")
    }
  }

  def addRoom(originRoom: Point, direction: Direction): Dungeon = {
    val newRoom = originRoom + direction

    if (roomGrid.contains(newRoom)) {
      throw new IllegalArgumentException(s"Room already exists at $newRoom")
    }

    copy(
      roomGrid = roomGrid + newRoom,
      roomConnections = roomConnections + RoomConnection(originRoom, direction, newRoom)
        + RoomConnection(newRoom, Direction.oppositeOf(direction), originRoom)
    )
  }

  val availableRooms: Set[(Point, Direction)] =
    roomGrid.flatMap { room =>
      Direction.values.map { direction =>
        (room, direction)
      }
    }.filterNot { case (room, direction) =>
      roomGrid.contains(room + direction) || blockedRooms.contains(room)
    }

  def availableRooms(room: Point): Set[(Point, Direction)] =
    availableRooms.filter {
      case (originRoom, _) =>
        room == originRoom
    }

  val doorPoints: Set[Point] = roomConnections.map {
    case RoomConnection(originRoom, direction, _, _) =>
      val originRoomX = originRoom.x * Dungeon.roomSize
      val originRoomY = originRoom.y * Dungeon.roomSize

      direction match {
        case Direction.Up => Point(originRoomX + Dungeon.roomSize / 2, originRoomY)
        case Direction.Down => Point(originRoomX + Dungeon.roomSize / 2, originRoomY + Dungeon.roomSize - 1)
        case Direction.Left => Point(originRoomX, originRoomY + Dungeon.roomSize / 2)
        case Direction.Right => Point(originRoomX + Dungeon.roomSize - 1, originRoomY + Dungeon.roomSize / 2)
      }
  }

  val lockedDoors: Set[(Point, LockedDoor)] = roomConnections.collect {
    case RoomConnection(originRoom, direction, _, Some(LockedDoor(keyColour))) =>
      val originRoomX = originRoom.x * Dungeon.roomSize
      val originRoomY = originRoom.y * Dungeon.roomSize

      val position = direction match {
        case Direction.Up => Point(originRoomX + Dungeon.roomSize / 2, originRoomY)
        case Direction.Down => Point(originRoomX + Dungeon.roomSize / 2, originRoomY + Dungeon.roomSize)
        case Direction.Left => Point(originRoomX, originRoomY + Dungeon.roomSize / 2)
        case Direction.Right => Point(originRoomX + Dungeon.roomSize, originRoomY + Dungeon.roomSize / 2)
      }
      position -> LockedDoor(keyColour)
  }

  val tiles: Map[Point, TileType] = roomGrid.flatMap {
    room =>
      val roomX = room.x * Dungeon.roomSize
      val roomY = room.y * Dungeon.roomSize

      def isWall(point: Point) = point.x == roomX || point.x == roomX + Dungeon.roomSize || point.y == roomY || point.y == roomY + Dungeon.roomSize

      def isDoor(point: Point) = doorPoints.contains(point)

      val roomTiles = for {
        x <- roomX until roomX + Dungeon.roomSize + 1
        y <- roomY until roomY + Dungeon.roomSize + 1
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

  val walls: Set[Point] = tiles.filter(_._2 == Wall).keySet

  val lockedDoorCount: Int = roomConnections.count(_.isLocked)

  val dungeonPath: Seq[RoomConnection] = RoomGridPathfinder.findPath(
    rooms = roomGrid,
    roomConnections = roomConnections,
    startPoint = startPoint,
    target = endpoint.getOrElse(startPoint)
  )

  def roomConnections(room: Point): Set[RoomConnection] = {
    roomConnections.filter(_.originRoom == room)
  }

  val keyRoomPaths: Set[Seq[RoomConnection]] = for {
    roomConnection@RoomConnection(originRoom, direction, destinationRoom, optLock) <- roomConnections
    if optLock.isDefined
    path = RoomGridPathfinder.findPath(
      rooms = roomGrid,
      roomConnections = roomConnections,
      startPoint = originRoom,
      target = destinationRoom
    )
  } yield path
}


case class RoomConnection(originRoom: Point, direction: Direction, destinationRoom: Point, optLock: Option[LockedDoor] = None) {
  def isLocked: Boolean = optLock.isDefined
}

object Dungeon {
  val roomSize = 12
}