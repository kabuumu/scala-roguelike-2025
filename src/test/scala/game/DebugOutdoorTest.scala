package game

import map.{Dungeon, MapGenerator, TileType}
import org.scalatest.funsuite.AnyFunSuite

class DebugOutdoorTest extends AnyFunSuite {
  
  test("Debug dungeon generation") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    println(s"\n=== DUNGEON DEBUG ===")
    println(s"Total rooms: ${dungeon.roomGrid.size}")
    println(s"Outdoor rooms: ${dungeon.outdoorRooms.size}")
    println(s"Dungeon rooms: ${dungeon.roomGrid.size - dungeon.outdoorRooms.size}")
    println(s"Starting point: ${dungeon.startPoint}")
    println(s"Endpoint: ${dungeon.endpoint}")
    
    println(s"\nAll rooms:")
    dungeon.roomGrid.toSeq.sortBy(p => (p.y, p.x)).foreach { room =>
      val isOutdoor = dungeon.outdoorRooms.contains(room)
      val isStart = room == dungeon.startPoint
      val isEnd = dungeon.endpoint.contains(room)
      val marker = if (isStart) " [START]" else if (isEnd) " [END]" else ""
      val typeStr = if (isOutdoor) "OUTDOOR" else "DUNGEON"
      println(s"  Room at $room - $typeStr$marker")
    }
    
    println(s"\nConnections: ${dungeon.roomConnections.size}")
    
    // Check if outdoor entrance connects to dungeon
    val outdoorEntranceRoom = dungeon.startPoint
    val connectionsFromOutdoor = dungeon.roomConnections.filter(_.originRoom == outdoorEntranceRoom)
    println(s"\nConnections from outdoor entrance ($outdoorEntranceRoom):")
    connectionsFromOutdoor.foreach { conn =>
      val destType = if (dungeon.outdoorRooms.contains(conn.destinationRoom)) "OUTDOOR" else "DUNGEON"
      val depth = dungeon.roomDepths.getOrElse(conn.destinationRoom, -1)
      println(s"  -> ${conn.direction} to ${conn.destinationRoom} ($destType, depth=$depth)")
    }
    
    val hasDungeonConnection = connectionsFromOutdoor.exists(conn => 
      !dungeon.outdoorRooms.contains(conn.destinationRoom)
    )
    
    // Find which dungeon room we connect to and its depth
    val connectedDungeonRoom = connectionsFromOutdoor
      .find(conn => !dungeon.outdoorRooms.contains(conn.destinationRoom))
      .map(_.destinationRoom)
    
    val connectedDepth = connectedDungeonRoom.flatMap(room => dungeon.roomDepths.get(room))
    
    println(s"\n*** Can access dungeon: $hasDungeonConnection ***")
    println(s"*** Connected to dungeon room: $connectedDungeonRoom at depth: $connectedDepth ***")
    
    // Show all dungeon room depths
    println(s"\nDungeon room depths:")
    dungeon.roomGrid.filterNot(dungeon.outdoorRooms.contains).foreach { room =>
      val depth = dungeon.roomDepths.getOrElse(room, -1)
      val isEndpoint = dungeon.endpoint.contains(room)
      val marker = if (isEndpoint) " [BOSS]" else ""
      println(s"  Room $room: depth $depth$marker")
    }
  }
}

