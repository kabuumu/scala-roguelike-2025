package map

import game.entity.EntityType.LockedDoor
import game.{Direction, Point}
import map.Dungeon.roomSize
import map.TileType._

case class Dungeon(roomGrid: Set[Point] = Set(Point(0, 0)),
                   roomConnections: Set[RoomConnection] = Set.empty,
                   blockedRooms: Set[Point] = Set.empty, //Rooms to no longer add connections to
                   startPoint: Point = Point(0, 0),
                   endpoint: Option[Point] = None,
                   items: Set[(Point, ItemDescriptor)] = Set.empty,
                   testMode: Boolean = false,
                   seed: Long = System.currentTimeMillis()) {
  def lockRoomConnection(roomConnection: RoomConnection, lock: LockedDoor): Dungeon = {
    copy(
      roomConnections = roomConnections - roomConnection + roomConnection.copy(
        optLock = Some(lock)
      )
    )
  }

  def addItem(room: Point, item: ItemDescriptor): Dungeon = {
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

  lazy val noise: Map[(Int, Int), Int] = {
    val minX: Int = roomGrid.map(_.x).min * roomSize
    val maxX: Int = roomGrid.map(_.x).max * roomSize + roomSize
    val minY: Int = roomGrid.map(_.y).min * roomSize
    val maxY: Int = roomGrid.map(_.y).max * roomSize + roomSize

    NoiseGenerator.getNoise(minX, maxX, minY, maxY, seed)
  }

  lazy val tiles: Map[Point, TileType] = roomGrid.flatMap {
    room =>
      val roomX = room.x * Dungeon.roomSize
      val roomY = room.y * Dungeon.roomSize

      def isWall(point: Point) = point.x == roomX || point.x == roomX + Dungeon.roomSize || point.y == roomY || point.y == roomY + Dungeon.roomSize

      def isDoor(point: Point) = doorPoints.contains(point)

      // If the point is the centre of a room, a door, or the path between, it must be a floor tile
      def mustBeFloor(point: Point): Boolean = {
        val roomCentre = Point(
          roomX + Dungeon.roomSize / 2,
          roomY + Dungeon.roomSize / 2
        )

        // Find all points between the centre of the room and any doors within the room
        val roomPaths = for {
          roomConnection <- roomConnections(room)
          doorPoint = roomConnection.direction match {
            case Direction.Up => Point(roomX + Dungeon.roomSize / 2, roomY)
            case Direction.Down => Point(roomX + Dungeon.roomSize / 2, roomY + Dungeon.roomSize)
            case Direction.Left => Point(roomX, roomY + Dungeon.roomSize / 2)
            case Direction.Right => Point(roomX + Dungeon.roomSize, roomY + Dungeon.roomSize / 2)
          }
          pathX <- (Math.min(roomCentre.x, doorPoint.x)) to (Math.max(roomCentre.x, doorPoint.x))
          pathY <- (Math.min(roomCentre.y, doorPoint.y)) to (Math.max(roomCentre.y, doorPoint.y))
          if roomCentre.x == doorPoint.x || roomCentre.y == doorPoint.y // Ensure we only consider horizontal or vertical paths
        } yield Point(pathX, pathY)

        roomPaths.contains(point)
      }

      val roomTiles = for {
        x <- roomX to roomX + Dungeon.roomSize
        y <- roomY to roomY + Dungeon.roomSize
      } yield {
        val point = Point(x, y)

        val roomConnectionsForWall = if(isWall(point)) getRoomConnectionsForWall(point) else Set.empty[RoomConnection]

        if (isDoor(point) || mustBeFloor(point)) noise(x -> y) match
          case _ if testMode => (point, TileType.Floor)
          case 0 | 1 => (point, TileType.Bridge)
          case 2 | 3 => (point, TileType.Floor)
          case 4 | 5 | 6 | 7 => (point, TileType.MaybeFloor)
        else if(isWall(point) && roomConnectionsForWall.exists(_.isLocked)) noise(x -> y) match
          case _ if testMode => (point, TileType.Wall)
          case 0 | 1 | 2 | 3 => (point, TileType.Water)
          case 4 | 5 | 6 | 7 => (point, TileType.Wall)
        else if(isWall(point) && roomConnectionsForWall.isEmpty)
          (point, TileType.Wall)
        else noise(x -> y) match
          case _ if testMode => (point, TileType.Floor)
          case 0 | 1 => (point, TileType.Water)
          case 2 | 3 => (point, TileType.Floor)
          case 4 | 5 => (point, TileType.MaybeFloor)
          case 6 | 7 => (point, TileType.Rock)
      }

      roomTiles.toMap
  }.toMap

  lazy val walls: Set[Point] = tiles.filter(_._2 == TileType.Wall).keySet
  
  lazy val rocks: Set[Point] = tiles.filter(_._2 == TileType.Rock).keySet

  lazy val water: Set[Point] = tiles.filter(_._2 == TileType.Water).keySet

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

  def wallIsOnLockedConnection(wall: Point): Boolean = {
    val roomConnectionsForWall = roomConnections.filter {
      case RoomConnection(originRoom, direction, destinationRoom, optLock) =>
        val originRoomX = originRoom.x * Dungeon.roomSize
        val originRoomY = originRoom.y * Dungeon.roomSize

        val isWithinXBounds = wall.x > originRoomX && wall.x < originRoomX + Dungeon.roomSize
        val isWithinYBounds = wall.y > originRoomY && wall.y < originRoomY + Dungeon.roomSize

        direction match {
          case Direction.Up => wall.y == originRoomY && isWithinXBounds
          case Direction.Down => wall.y == originRoomY + Dungeon.roomSize && isWithinXBounds
          case Direction.Left => wall.x == originRoomX && isWithinYBounds
          case Direction.Right => wall.x == originRoomX + Dungeon.roomSize && isWithinYBounds
        }
    }

    roomConnectionsForWall.exists(_.isLocked) || roomConnectionsForWall.isEmpty
  }

  def getRoomConnectionsForWall(wall: Point) = roomConnections.filter {
    case RoomConnection(originRoom, direction, destinationRoom, optLock) =>
      val originRoomX = originRoom.x * Dungeon.roomSize
      val originRoomY = originRoom.y * Dungeon.roomSize

      val isWithinXBounds = wall.x > originRoomX && wall.x < originRoomX + Dungeon.roomSize
      val isWithinYBounds = wall.y > originRoomY && wall.y < originRoomY + Dungeon.roomSize

      direction match {
        case Direction.Up => wall.y == originRoomY && isWithinXBounds
        case Direction.Down => wall.y == originRoomY + Dungeon.roomSize && isWithinXBounds
        case Direction.Left => wall.x == originRoomX && isWithinYBounds
        case Direction.Right => wall.x == originRoomX + Dungeon.roomSize && isWithinYBounds
      }
  }
  
  val nonKeyItems: Set[(Point, ItemDescriptor)] = items.filterNot {
    case (_, ItemDescriptor.KeyDescriptor(_)) => true
    case _ => false
  }
}


case class RoomConnection(originRoom: Point, direction: Direction, destinationRoom: Point, optLock: Option[LockedDoor] = None) {
  def isLocked: Boolean = optLock.isDefined
}

object Dungeon {
  val roomSize = 10
}