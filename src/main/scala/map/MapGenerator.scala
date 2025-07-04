package map

import game.Item.Key

import scala.annotation.tailrec

object MapGenerator {
  def generateDungeon(dungeonSize: Int, lockedDoorCount: Int = 0, itemCount: Int = 0): Dungeon = {
    val startTime = System.currentTimeMillis()

    val mutators: Set[DungeonMutator] = Set(
      new EndPointMutator(dungeonSize),
      new KeyLockMutator(lockedDoorCount, dungeonSize),
      new TreasureRoomMutator(itemCount, dungeonSize),
    )

    @tailrec
    def recursiveGenerator(openDungeons: Set[Dungeon]): Dungeon = {
      val currentDungeon: Dungeon = openDungeons.find(_.endpoint.isDefined) match {
        case Some(dungeonWithEndpoint) => dungeonWithEndpoint
        case None => openDungeons.maxByOption(_.roomGrid.size) match {
          case Some(openDungeon) => openDungeon
          case None => throw new IllegalStateException("No open dungeons available")
        }
      }

      val newOpenDungeons: Set[Dungeon] = for {
        mutator <- mutators
        possibleDungeon <- mutator.getPossibleMutations(currentDungeon)
      } yield possibleDungeon
      
      newOpenDungeons.find(dungeon =>
        dungeon.lockedDoorCount == lockedDoorCount
          && dungeon.nonKeyItems.size == itemCount
//          && dungeon.dungeonPath.size == dungeonPathSize
          && dungeon.roomGrid.size == dungeonSize
      ) match {
        case Some(completedDungeon) =>
          println(s"Completed dungeon took ${System.currentTimeMillis() - startTime}ms")

          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon)
      }
    }

    recursiveGenerator(Set(Dungeon()))
  }
}