package map

import game.{Direction, Point}

case class Room(x: Int, y: Int, height: Int, width: Int, doors: Set[Direction]) {
  private def isDoor(tileLocation: Point): Boolean = doors.map {
    case Direction.Up => Point(x + width / 2, y)
    case Direction.Down => Point(x + width / 2, y + height - 1)
    case Direction.Left => Point(x, y + height / 2)
    case Direction.Right => Point(x + width - 1, y + height / 2)
  }.contains(tileLocation)

  private def isWall(tileLocation: Point): Boolean =
    tileLocation.x == x || tileLocation.x == x + width - 1 || tileLocation.y == y || tileLocation.y == y + height - 1

  val gameMap: GameMap = {
    val tiles = for {
      x <- x until x + width
      y <- y until y + height
    } yield Point(x, y) match {
      case point if isDoor(point) => (point, TileType.Floor)
      case point if isWall(point) => (point, TileType.Wall)
      case point => (point, TileType.Floor)
    }

    GameMap(tiles)
  }
}

case class TreeRoom(x: Int, y: Int, linkedRoom: Map[Direction, String] = Map.empty) {
  val asRoom: Room = Room(x, y, 9, 9, linkedRoom.keys.toSet)
}
case class RoomTree(rooms: Map[String, TreeRoom]) {
  def addInitialRoom(roomId: String, room: TreeRoom): RoomTree = copy(rooms = rooms + (roomId -> room))
  def addRoom(roomId: String, direction: Direction, newRoomId: String): RoomTree = {
    val existingRoom = rooms(roomId)

    val(x, y) = direction match {
      case Direction.Up => (existingRoom.x, existingRoom.y - 8)
      case Direction.Down => (existingRoom.x, existingRoom.y + existingRoom.asRoom.height - 1)
      case Direction.Left => (existingRoom.x - 8, existingRoom.y)
      case Direction.Right => (existingRoom.x + existingRoom.asRoom.width - 1, existingRoom.y)
    }

    val newRoom = TreeRoom(x, y, linkedRoom = Map(Direction.oppositeOf(direction) -> roomId))

    val updatedRoom = existingRoom.copy(linkedRoom = existingRoom.linkedRoom + (direction -> newRoomId))
    copy(rooms = rooms + (roomId -> updatedRoom) + (newRoomId -> newRoom))
  }

  val tiles: Seq[(Point, TileType)] = {
    rooms.values.foreach(room => println(room.asRoom.gameMap))

    rooms.values.flatMap(_.asRoom.gameMap.tiles).toSeq
  }
}