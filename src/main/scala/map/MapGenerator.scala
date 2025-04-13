package map

import scala.annotation.tailrec
import scala.util.Random

object MapGenerator {
  def generateRandomDungeon(dungeonSize: Int, seed: Long): Dungeon = {
    val random = new Random(seed)

    @tailrec
    def addRoom(dungeon: Dungeon): Dungeon = {
      val availableRooms = dungeon.availableRooms.toSeq
      val randomRoomIndex = random.nextInt(availableRooms.size)

      val (room, direction) = availableRooms(randomRoomIndex)
      val newDungeon = dungeon.addRoom(room, direction)

      if (newDungeon.roomGrid.size < dungeonSize) addRoom(newDungeon)
      else newDungeon
    }

    addRoom(Dungeon())
  }

  //Create empty dungeon
  //Run through all dungeon mutators (currently just create room)
  //Run each possible dungeon through pathfinder to check it is completable
  //Return the dungeons that are completable
  //If any dungeons meet all completion criteria (currently just size), return them
  def generateDungeon(dungeonSize: Int): Dungeon = {
    @tailrec
    def recursiveGenerator(openDungeons: Set[Dungeon]): Dungeon = {
      val newOpenDungeons = for {
        dungeon <- openDungeons
        (originRoom, direction) <- dungeon.availableRooms
      } yield dungeon.addRoom(originRoom, direction)

      newOpenDungeons.find(_.roomGrid.size == 10) match {
        case Some(completedDungeon) =>
          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons)
      }
    }

    recursiveGenerator(Set(Dungeon()))
  }
}