package game

import map.{Dungeon, MapGenerator, TileType}
import org.scalatest.funsuite.AnyFunSuite

class DebugOutdoorTest extends AnyFunSuite {

  test("Debug dungeon generation") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)

    println(s"\n=== DUNGEON DEBUG ===")
    println(s"Total rooms: ${dungeon.roomGrid.size}")
    println(s"Starting point: ${dungeon.startPoint}")
    println(s"Endpoint: ${dungeon.endpoint}")

    println(s"\nAll rooms:")
    dungeon.roomGrid.toSeq.sortBy(p => (p.y, p.x)).foreach { room =>
      val isStart = room == dungeon.startPoint
      val isEnd = dungeon.endpoint.contains(room)
      val marker = if (isStart) " [START]" else if (isEnd) " [END]" else ""
      println(s"  Room at $room$marker")
    }

    println(s"\nConnections: ${dungeon.roomConnections.size}")

    // Check connections from start room
    val startRoom = dungeon.startPoint
    val connectionsFromStart = dungeon.roomConnections.filter(_.originRoom == startRoom)
    println(s"\nConnections from start room ($startRoom):")
    connectionsFromStart.foreach { conn =>
      val depth = dungeon.roomDepths.getOrElse(conn.destinationRoom, -1)
      println(s"  -> ${conn.direction} to ${conn.destinationRoom} (depth=$depth)")
    }

    println(s"\nRoom depths: ${dungeon.roomDepths.size} rooms with depth values")
    dungeon.roomDepths.toSeq.sortBy(_._2).foreach { case (room, depth) =>
      val isStart = room == startRoom
      val isEndpoint = dungeon.endpoint.contains(room)
      val marker = if (isStart) " [START]" else if (isEndpoint) " [BOSS]" else ""
      println(s"  Room $room: depth=$depth$marker")
    }
  }
}

