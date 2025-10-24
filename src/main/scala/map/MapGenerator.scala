package map

import game.Point
import scala.annotation.tailrec

object MapGenerator {
  def generateDungeon(dungeonSize: Int, lockedDoorCount: Int = 0, itemCount: Int = 0, seed: Long = System.currentTimeMillis()): Dungeon = {
    val startTime = System.currentTimeMillis()

    val mutators: Set[DungeonMutator] = Set(
      new EndPointMutator(dungeonSize),
      new TraderRoomMutator(dungeonSize),
      new KeyLockMutator(lockedDoorCount, dungeonSize),
      new TreasureRoomMutator(itemCount, dungeonSize),
      new BossRoomMutator(dungeonSize)
    )

    @tailrec
    def recursiveGenerator(openDungeons: Set[Dungeon]): Dungeon = {
      val currentDungeon: Dungeon = openDungeons.maxBy( dungeon =>
        dungeon.roomGrid.size + dungeon.lockedDoorCount + dungeon.nonKeyItems.size
      )

      val newOpenDungeons: Set[Dungeon] = for {
        mutator <- mutators
        possibleDungeon <- mutator.getPossibleMutations(currentDungeon)
      } yield possibleDungeon
      
      newOpenDungeons.find(dungeon =>
        dungeon.lockedDoorCount == lockedDoorCount
          && dungeon.nonKeyItems.size == itemCount
//          && dungeon.dungeonPath.size == dungeonPathSize
          && dungeon.roomGrid.size == dungeonSize
          && dungeon.traderRoom.isDefined
          && dungeon.hasBossRoom
      ) match {
        case Some(completedDungeon) =>
          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon)
      }
    }

    val baseDungeon = recursiveGenerator(Set(Dungeon(seed = seed)))
    
    // Add outdoor rooms connected to the starting room
    val dungeonWithOutdoor = addOutdoorRooms(baseDungeon)

    println("Generating Dungeon")
    println(s"  Generated dungeon with ${dungeonWithOutdoor.roomGrid.size} rooms " +
      s"(${dungeonWithOutdoor.outdoorRooms.size} outdoor), " +
      s"${dungeonWithOutdoor.roomConnections.size} connections, " +
      s"${dungeonWithOutdoor.lockedDoorCount} locked doors, " +
      s"${dungeonWithOutdoor.nonKeyItems.size} items")

    println(s"  Completed dungeon with config took ${System.currentTimeMillis() - startTime}ms")

    dungeonWithOutdoor
  }
  
  /**
   * Add outdoor rooms around the dungeon entrance.
   * Creates an outdoor entrance room below the starting room, then surrounds it with more outdoor rooms.
   */
  private def addOutdoorRooms(dungeon: Dungeon): Dungeon = {
    import game.Direction
    
    // Find the highest Y coordinate in the dungeon (bottom of dungeon in screen space)
    val maxY = dungeon.roomGrid.map(_.y).max
    
    // Shift entire dungeon DOWN so it starts at Y=0 or negative Y
    // Outdoor rooms will be placed at higher Y values (below dungeon visually)
    val yShift = -maxY - 1  // Shift so maximum Y becomes -1 (dungeon is at Y <= -1)
    
    val shiftedDungeon = dungeon.copy(
      roomGrid = dungeon.roomGrid.map(p => Point(p.x, p.y + yShift)),
      startPoint = Point(dungeon.startPoint.x, dungeon.startPoint.y + yShift),
      endpoint = dungeon.endpoint.map(p => Point(p.x, p.y + yShift)),
      traderRoom = dungeon.traderRoom.map(p => Point(p.x, p.y + yShift)),
      roomConnections = dungeon.roomConnections.map(rc => 
        rc.copy(
          originRoom = Point(rc.originRoom.x, rc.originRoom.y + yShift),
          destinationRoom = Point(rc.destinationRoom.x, rc.destinationRoom.y + yShift)
        )
      ),
      items = dungeon.items.map { case (p, item) => (Point(p.x, p.y + yShift), item) }
    )
    
    // Find the dungeon room with depth 1 (the original starting room, actual dungeon entrance)
    // We need to calculate depths BEFORE changing the startPoint
    val dungeonDepths = shiftedDungeon.roomDepths
    
    // Find the dungeon room with the minimum depth (should be depth 0 for original startPoint)
    // We want the room with depth 1 if it exists, otherwise depth 0
    val dungeonEntranceRoom = shiftedDungeon.roomGrid
      .filter(room => !shiftedDungeon.outdoorRooms.contains(room))
      .toSeq
      .sortBy(room => dungeonDepths.getOrElse(room, Int.MaxValue))
      .headOption
      .getOrElse(shiftedDungeon.startPoint)
    
    // Add outdoor entrance room at Y=0, aligned with the dungeon entrance X coordinate
    val outdoorEntranceRoom = Point(dungeonEntranceRoom.x, 0)
    
    val withEntranceRoom = shiftedDungeon.copy(
      roomGrid = shiftedDungeon.roomGrid + outdoorEntranceRoom,
      outdoorRooms = Set(outdoorEntranceRoom),
      roomConnections = shiftedDungeon.roomConnections + 
        RoomConnection(dungeonEntranceRoom, Direction.Down, outdoorEntranceRoom) +
        RoomConnection(outdoorEntranceRoom, Direction.Up, dungeonEntranceRoom)
    )
    
    // Add outdoor rooms surrounding the entrance (left, right, down for more outdoor area)
    val outdoorLeft = Point(outdoorEntranceRoom.x - 1, outdoorEntranceRoom.y)
    val outdoorRight = Point(outdoorEntranceRoom.x + 1, outdoorEntranceRoom.y)
    val outdoorDown = Point(outdoorEntranceRoom.x, outdoorEntranceRoom.y + 1)
    
    // Add left outdoor room
    val withLeft = withEntranceRoom.copy(
      roomGrid = withEntranceRoom.roomGrid + outdoorLeft,
      outdoorRooms = withEntranceRoom.outdoorRooms + outdoorLeft,
      roomConnections = withEntranceRoom.roomConnections +
        RoomConnection(outdoorEntranceRoom, Direction.Left, outdoorLeft) +
        RoomConnection(outdoorLeft, Direction.Right, outdoorEntranceRoom)
    )
    
    // Add right outdoor room
    val withRight = withLeft.copy(
      roomGrid = withLeft.roomGrid + outdoorRight,
      outdoorRooms = withLeft.outdoorRooms + outdoorRight,
      roomConnections = withLeft.roomConnections +
        RoomConnection(outdoorEntranceRoom, Direction.Right, outdoorRight) +
        RoomConnection(outdoorRight, Direction.Left, outdoorEntranceRoom)
    )
    
    // Add bottom outdoor room (more outdoor area below entrance)
    val withBottom = withRight.copy(
      roomGrid = withRight.roomGrid + outdoorDown,
      outdoorRooms = withRight.outdoorRooms + outdoorDown,
      roomConnections = withRight.roomConnections +
        RoomConnection(outdoorEntranceRoom, Direction.Down, outdoorDown) +
        RoomConnection(outdoorDown, Direction.Up, outdoorEntranceRoom)
    )
    
    // Add corner outdoor rooms (below-left and below-right)
    val outdoorLeftDown = Point(outdoorLeft.x, outdoorLeft.y + 1)
    val outdoorRightDown = Point(outdoorRight.x, outdoorRight.y + 1)
    
    val withLeftDown = withBottom.copy(
      roomGrid = withBottom.roomGrid + outdoorLeftDown,
      outdoorRooms = withBottom.outdoorRooms + outdoorLeftDown,
      roomConnections = withBottom.roomConnections +
        RoomConnection(outdoorLeft, Direction.Down, outdoorLeftDown) +
        RoomConnection(outdoorLeftDown, Direction.Up, outdoorLeft) +
        RoomConnection(outdoorDown, Direction.Left, outdoorLeftDown) +
        RoomConnection(outdoorLeftDown, Direction.Right, outdoorDown)
    )
    
    val withRightDown = withLeftDown.copy(
      roomGrid = withLeftDown.roomGrid + outdoorRightDown,
      outdoorRooms = withLeftDown.outdoorRooms + outdoorRightDown,
      roomConnections = withLeftDown.roomConnections +
        RoomConnection(outdoorRight, Direction.Down, outdoorRightDown) +
        RoomConnection(outdoorRightDown, Direction.Up, outdoorRight) +
        RoomConnection(outdoorDown, Direction.Right, outdoorRightDown) +
        RoomConnection(outdoorRightDown, Direction.Left, outdoorDown)
    )
    
    // Change starting point to be the outdoor entrance room
    // Player starts outdoors and can go UP to enter the dungeon
    withRightDown.copy(startPoint = outdoorEntranceRoom)
  }
}