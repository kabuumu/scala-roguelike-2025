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
      val currentDungeon = openDungeons.maxBy(_.roomGrid.size)

      val newOpenDungeons = for {
        (originRoom, direction) <- currentDungeon.availableRooms
      } yield currentDungeon.addRoom(originRoom, direction)


      newOpenDungeons.find(_.roomGrid.size == dungeonSize) match {
        case Some(completedDungeon) =>
          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon)
      }
    }

    recursiveGenerator(Set(Dungeon()))
  }
}