package map

import game.Direction.*
import game.{Direction, Point}
import scala.util.Random

import scala.annotation.tailrec

object MapGenerator {
  def generateRoomTree(): RoomTree = {
    val roomTree = RoomTree(Map.empty)
      .addInitialRoom("room1", TreeRoom(0, 0))
      .addRoom("room1", Right, "room2")
      .addRoom("room2", Down, "room3")
      .addRoom("room3", Left, "room4")
      .addRoom("room2", Up, "room5")

    roomTree
  }

  def generateDungeon(dungeonSize: Int, seed: Long): Dungeon = {
    val random = new Random(seed)

    @tailrec
    def addRoom(dungeon: Dungeon): Dungeon = {
      val availableRooms = dungeon.availableRooms.toSeq
      val randomRoomIndex = random.nextInt(availableRooms.size)

      val (room, direction) = availableRooms(randomRoomIndex)
      val newDungeon = dungeon.addRoom(room, direction)

      if(newDungeon.roomGrid.size < dungeonSize) addRoom(newDungeon)
      else newDungeon
    }

    addRoom(Dungeon())
  }


}