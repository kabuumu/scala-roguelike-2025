package map

import scala.annotation.tailrec

object MapGenerator {

  //Create empty dungeon
  //Run through all dungeon mutators (currently just create room)
  //Run each possible dungeon through pathfinder to check it is completable
  //Return the dungeons that are completable
  //If any dungeons meet all completion criteria (currently just size), return them
  def generateDungeon(dungeonSize: Int, lockedDoorCount: Int): Dungeon = {
    val dungeonPathSize = dungeonSize / 2

    val mutators: Set[DungeonMutator] = Set(
      //      new NewRoomMutator(dungeonSize),
      new EndPointMutator(dungeonPathSize),
      //      new KeyLockMutator(lockedDoorCount)
    )

    @tailrec
    def recursiveGenerator(openDungeons: Set[Dungeon]): Dungeon = {
      val currentDungeon: Dungeon = openDungeons.find(_.endpoint.isDefined) match {
        case Some(dungeonWithEndpoint) => dungeonWithEndpoint
        case None => openDungeons.maxBy(_.roomGrid.size)
      }

      val newOpenDungeons: Set[Dungeon] = for {
        mutator <- mutators
        possibleDungeon <- mutator.getPossibleMutations(currentDungeon)
      } yield possibleDungeon

      newOpenDungeons.find(dungeon =>
        dungeon.dungeonPath.size == dungeonPathSize
//          && dungeon.lockedDoorCount == lockedDoorCount
        //        && dungeon.roomGrid.size == dungeonSize
      ) match {
        case Some(completedDungeon) =>
          println(s"Completed dungeon has locked doors at ${completedDungeon.lockedDoors}")
          println(s"Completed dungeon has keys at ${completedDungeon.items}")

          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon)
      }
    }


    recursiveGenerator(Set(Dungeon()))
  }
}