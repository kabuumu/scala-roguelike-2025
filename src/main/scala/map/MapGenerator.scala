package map

import game.Direction.*
import game.{Direction, Point}

object MapGenerator {

  def generate(mapWidth: Int, mapHeight: Int): GameMap = {
    GameMap(Seq.empty)
      .addRoom(Room(0, 0, 11, 8, Set(Right)))
      .addRoom(Room(10, 0, 11, 8, Set(Left, Down)))
      .addRoom(Room(0, 7, 11, 8, Set(Right)))
      .addRoom(Room(10, 7, 11, 8, Set(Left, Up)))
  }

  def generateRoomTree(): RoomTree = {
    val roomTree = RoomTree(Map.empty)
      .addInitialRoom("room1", TreeRoom(0, 0))
      .addRoom("room1", Right, "room2")
      .addRoom("room2", Down, "room3")
      .addRoom("room3", Left, "room4")
      .addRoom("room2", Up, "room5")

    roomTree
  }


}