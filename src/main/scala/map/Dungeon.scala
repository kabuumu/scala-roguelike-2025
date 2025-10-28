package map

import data.Items.ItemReference
import game.entity.EntityType.LockedDoor
import game.{Direction, Point}
import map.Dungeon.roomSize
import map.TileType._

case class Dungeon(roomGrid: Set[Point] = Set(Point(0, 0)),
                   roomConnections: Set[RoomConnection] = Set.empty,
                   blockedRooms: Set[Point] = Set.empty, //Rooms to no longer add connections to
                   startPoint: Point = Point(0, 0),
                   endpoint: Option[Point] = None,
                   items: Set[(Point, ItemReference)] = Set.empty,
                   traderRoom: Option[Point] = None,
                   hasBossRoom: Boolean = false,
                   outdoorRooms: Set[Point] = Set.empty, // Rooms that are outdoor areas
                   testMode: Boolean = false,
                   seed: Long = System.currentTimeMillis()) {
  def lockRoomConnection(roomConnection: RoomConnection, lock: LockedDoor): Dungeon = {
    copy(
      roomConnections = roomConnections - roomConnection + roomConnection.copy(
        optLock = Some(lock)
      )
    )
  }

  def addItem(room: Point, item: ItemReference): Dungeon = {
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

  lazy val tiles: Map[Point, TileType] = {
    // Calculate the bounds of the entire dungeon
    val minRoomX = roomGrid.map(_.x).min
    val maxRoomX = roomGrid.map(_.x).max
    val minRoomY = roomGrid.map(_.y).min
    val maxRoomY = roomGrid.map(_.y).max
    
    // Create outdoor perimeter tiles around the dungeon (2 room-widths of padding)
    val outdoorPadding = 2
    val dungeonMinX = minRoomX * Dungeon.roomSize
    val dungeonMaxX = (maxRoomX + 1) * Dungeon.roomSize
    val dungeonMinY = minRoomY * Dungeon.roomSize
    val dungeonMaxY = (maxRoomY + 1) * Dungeon.roomSize
    
    val outdoorMinX = dungeonMinX - (outdoorPadding * Dungeon.roomSize)
    val outdoorMaxX = dungeonMaxX + (outdoorPadding * Dungeon.roomSize)
    val outdoorMinY = dungeonMinY - (outdoorPadding * Dungeon.roomSize)
    val outdoorMaxY = dungeonMaxY + (outdoorPadding * Dungeon.roomSize)
    
    // Generate outdoor area tiles - this creates a COMPLETE grid that covers everything
    val outdoorTiles = (for {
      x <- outdoorMinX to outdoorMaxX
      y <- outdoorMinY to outdoorMaxY
      point = Point(x, y)
    } yield {
      val isPerimeter = x == outdoorMinX || x == outdoorMaxX || y == outdoorMinY || y == outdoorMaxY
      
      if (isPerimeter) {
        // Outer perimeter is all trees (impassable boundary)
        (point, TileType.Tree)
      } else {
        // Inner outdoor area uses grass tiles with variety
        noise.getOrElse(x -> y, (x + y) % 8) match {
          case 0 | 1 | 2 => (point, TileType.Grass1)
          case 3 | 4 | 5 => (point, TileType.Grass2)
          case 6 | 7 => (point, TileType.Grass3)
          case _ => (point, TileType.Grass1)
        }
      }
    }).toMap
    
    // Generate regular dungeon room tiles
    val regularTiles = roomGrid.flatMap {
      room =>
        val roomX = room.x * Dungeon.roomSize
        val roomY = room.y * Dungeon.roomSize

        def isWall(point: Point) = point.x == roomX || point.x == roomX + Dungeon.roomSize || point.y == roomY || point.y == roomY + Dungeon.roomSize

        def isDoor(point: Point) = doorPoints.contains(point)

        val isBossRoom = hasBossRoom && endpoint.contains(room)
        val isTraderRoom = traderRoom.contains(room)
        val isStartingRoom = room == startPoint
        val isOutdoorRoom = outdoorRooms.contains(room)
        
        // Check if this room has connections to dungeon rooms (for outdoor rooms)
        // or connections to outdoor rooms (for dungeon rooms)
        def connectsToOutdoorRoom(direction: game.Direction): Boolean = {
          roomConnections.exists { conn =>
            conn.originRoom == room && 
            conn.direction == direction && 
            outdoorRooms.contains(conn.destinationRoom)
          }
        }
        
        def connectsToDungeonRoom(direction: game.Direction): Boolean = {
          roomConnections.exists { conn =>
            conn.originRoom == room && 
            conn.direction == direction && 
            !outdoorRooms.contains(conn.destinationRoom)
          }
        }

        // If the point is the centre of a room, a door, or the path between, it must be a floor tile
        def mustBeFloor(point: Point): Boolean = {
          val roomCentre = Point(
            roomX + Dungeon.roomSize / 2,
            roomY + Dungeon.roomSize / 2
          )

          // If this is the endpoint room (boss room) and we have a boss room, make the entire room floor
          if ((isBossRoom || isTraderRoom || isStartingRoom) && !isWall(point)) {
            true  // All non-wall tiles in boss room, trader room, or starting room should be floor
          } else {
            // Ensure room center and orthogonal adjacent tiles are always walkable for enemy placement
            val roomCenterArea = Set(
              roomCentre,                                    // Center
              Point(roomCentre.x - 1, roomCentre.y),       // Left
              Point(roomCentre.x + 1, roomCentre.y),       // Right
              Point(roomCentre.x, roomCentre.y - 1),       // Up
              Point(roomCentre.x, roomCentre.y + 1)        // Down
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

            roomCenterArea.contains(point) || roomPaths.contains(point)
          }
        }

        val roomTiles = for {
          x <- roomX to roomX + Dungeon.roomSize
          y <- roomY to roomY + Dungeon.roomSize
        } yield {
          val point = Point(x, y)

          val roomConnectionsForWall = if(isWall(point)) getRoomConnectionsForWall(point) else Set.empty[RoomConnection]

        // Special handling for outdoor rooms - use grass tiles and trees for walls
        if (isOutdoorRoom) {
          if (isDoor(point)) {
            // Check if this door connects to a dungeon room
            val doorConnectsToDungeon = roomConnectionsForWall.exists(conn => 
              !outdoorRooms.contains(conn.destinationRoom)
            )
            if (doorConnectsToDungeon) {
              // Doorway to dungeon uses dungeon floor
              (point, TileType.Floor)
            } else {
              // Doorway to other outdoor room uses dirt
              (point, TileType.Dirt)
            }
          } else if (isWall(point) && roomConnectionsForWall.isEmpty) {
              // Check if this wall is on the side that connects to a dungeon room
              val wallDirection = if (point.y == roomY) game.Direction.Up
                                 else if (point.y == roomY + Dungeon.roomSize) game.Direction.Down
                                 else if (point.x == roomX) game.Direction.Left
                                 else game.Direction.Right
              
              if (connectsToDungeonRoom(wallDirection)) {
                // Use solid wall (impassable) where outdoor connects to dungeon
                (point, TileType.Wall)
              } else {
                // Other outdoor walls are trees
                (point, TileType.Tree)
              }
            } else if (isWall(point) && roomConnectionsForWall.nonEmpty) {
              // Walls with connections - ensure they're solid walls to dungeon
              val hasConnectionToDungeon = roomConnectionsForWall.exists(conn => 
                !outdoorRooms.contains(conn.destinationRoom)
              )
              if (hasConnectionToDungeon) {
                (point, TileType.Wall)
              } else {
                (point, TileType.Dirt) // Connection to other outdoor room
              }
            } else {
              // Floor uses grass tiles with variety
              noise(x -> y) match {
                case 0 | 1 | 2 => (point, TileType.Grass1)
                case 3 | 4 | 5 => (point, TileType.Grass2)
                case 6 | 7 => (point, TileType.Grass3)
                case _ => (point, TileType.Grass1)
              }
            }
        } else if (isStartingRoom && outdoorRooms.nonEmpty) {
          // Only apply special starting room treatment if there are outdoor rooms
          // (for backward compatibility with old dungeons that have outdoor areas)
          if (isDoor(point)) {
            // Check if this door connects to a dungeon room (non-outdoor room)
            val doorConnectsToDungeon = roomConnectionsForWall.exists(conn => 
              !outdoorRooms.contains(conn.destinationRoom)
            )
            if (doorConnectsToDungeon) {
              // Doorway to dungeon uses dungeon floor
              (point, TileType.Floor)
            } else {
              // Doorway to other outdoor room uses dirt
              (point, TileType.Dirt)
            }
          } else if (isWall(point) && roomConnectionsForWall.isEmpty) {
              // Starting room walls are GRASS (passable, open to outdoor area)
              noise(x -> y) match {
                case 0 | 1 | 2 => (point, TileType.Grass1)
                case 3 | 4 | 5 => (point, TileType.Grass2)
                case 6 | 7 => (point, TileType.Grass3)
                case _ => (point, TileType.Grass1)
              }
            } else if (isWall(point) && roomConnectionsForWall.nonEmpty) {
              // Walls with doors/connections use dirt as transition to dungeon
              (point, TileType.Dirt)
            } else if (mustBeFloor(point)) {
              // Use grass tiles for the floor with some variety
              noise(x -> y) match {
                case 0 | 1 | 2 => (point, TileType.Grass1)
                case 3 | 4 | 5 => (point, TileType.Grass2)
                case 6 | 7 => (point, TileType.Grass3)
              }
            } else {
              // Default to grass
              (point, TileType.Grass1)
            }
          } else if (isDoor(point) || mustBeFloor(point)) noise(x -> y) match
            case _ if testMode || isBossRoom || isTraderRoom => (point, TileType.Floor)
            case 0 | 1 => (point, TileType.Bridge)
            case 2 | 3 => (point, TileType.Floor)
            case 4 | 5 | 6 | 7 => (point, TileType.MaybeFloor)
          else if(isWall(point) && (isBossRoom || isTraderRoom))
            (point, TileType.Wall)
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
    
    // Combine outdoor tiles with dungeon room tiles (room tiles override outdoor tiles)
    outdoorTiles ++ regularTiles
  }

  lazy val walls: Set[Point] = tiles.filter(t => t._2 == TileType.Wall || t._2 == TileType.Tree).keySet
  
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
  
  val nonKeyItems: Set[(Point, ItemReference)] = items.filterNot {
    case (_, ItemReference.YellowKey | ItemReference.BlueKey | ItemReference.RedKey) => true
    case _ => false
  }
  
  /**
   * Calculate the dungeon depth for each room based on shortest path distance from start point.
   * Returns a map from room Point to its depth (distance from start).
   * Note: Outdoor rooms are excluded from depth calculation.
   */
  lazy val roomDepths: Map[Point, Int] = {
    def calculateDepthFromStart(start: Point): Map[Point, Int] = {
      val visited = scala.collection.mutable.Set[Point]()
      val depths = scala.collection.mutable.Map[Point, Int]()
      val queue = scala.collection.mutable.Queue[(Point, Int)]()
      
      // If start is an outdoor room, find the first dungeon room connected to it
      val dungeonStart = if (outdoorRooms.contains(start)) {
        roomConnections
          .find(conn => conn.originRoom == start && !outdoorRooms.contains(conn.destinationRoom))
          .map(_.destinationRoom)
          .getOrElse(start)
      } else {
        start
      }
      
      // Only add to depths if it's not an outdoor room
      if (!outdoorRooms.contains(dungeonStart)) {
        queue.enqueue((dungeonStart, 0))
        depths(dungeonStart) = 0
        visited += dungeonStart
      }
      
      while (queue.nonEmpty) {
        val (currentRoom, currentDepth) = queue.dequeue()
        
        // Find all connected rooms from current room, excluding outdoor rooms
        val connectedRooms = roomConnections
          .filter(_.originRoom == currentRoom)
          .map(_.destinationRoom)
          .filterNot(visited.contains)
          .filterNot(outdoorRooms.contains) // Don't include outdoor rooms in depth calculation
        
        connectedRooms.foreach { nextRoom =>
          if (!visited.contains(nextRoom) && !outdoorRooms.contains(nextRoom)) {
            visited += nextRoom
            val nextDepth = currentDepth + 1
            depths(nextRoom) = nextDepth
            queue.enqueue((nextRoom, nextDepth))
          }
        }
      }
      
      depths.toMap
    }
    
    calculateDepthFromStart(startPoint)
  }
}


case class RoomConnection(originRoom: Point, direction: Direction, destinationRoom: Point, optLock: Option[LockedDoor] = None) {
  def isLocked: Boolean = optLock.isDefined
}

object Dungeon {
  val roomSize = 10
}