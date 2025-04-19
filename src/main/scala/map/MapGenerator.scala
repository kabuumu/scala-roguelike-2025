package map

import game.Item.Potion

import scala.annotation.tailrec

object MapGenerator {
  def generateDungeon(dungeonSize: Int, lockedDoorCount: Int): Dungeon = {
    val startTime = System.currentTimeMillis()
    val dungeonPathSize = dungeonSize / 2

    val mutators: Set[DungeonMutator] = Set(
      new EndPointMutator(dungeonPathSize),
      new KeyLockMutator(lockedDoorCount),
      new TreasureRoomMutator(3, dungeonPathSize),
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
          && dungeon.lockedDoorCount == lockedDoorCount
          && dungeon.items.count(_._2 == Potion) == 3
        //        && dungeon.roomGrid.size == dungeonSize
      ) match {
        case Some(completedDungeon) =>
          println(s"Completed dungeon has locked doors at ${completedDungeon.lockedDoors}")
          println(s"Completed dungeon has keys at ${completedDungeon.items}")
          println(s"Completed dungeon took ${System.currentTimeMillis() - startTime}ms")

          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon)
      }
    }

    recursiveGenerator(Set(Dungeon()))
  }
}