package map

import scala.annotation.tailrec

object MapGenerator {
  def generateDungeon(dungeonSize: Int, lockedDoorCount: Int = 0, itemCount: Int = 0, seed: Long = System.currentTimeMillis()): Dungeon = {
    val startTime = System.currentTimeMillis()

    val mutators: Set[DungeonMutator] = Set(
      new EndPointMutator(dungeonSize),
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
          && dungeon.hasBossRoom
      ) match {
        case Some(completedDungeon) =>
          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon)
      }
    }

    val dungeon = recursiveGenerator(Set(Dungeon(seed = seed)))

    println("Generating Dungeon")
    println(s"  Generated dungeon with ${dungeon.roomGrid.size} rooms, " +
      s"${dungeon.roomConnections.size} connections, " +
      s"${dungeon.lockedDoorCount} locked doors, " +
      s"${dungeon.nonKeyItems.size} items, ")
    
    println(s"  Completed dungeon with config took ${System.currentTimeMillis() - startTime}ms")

    dungeon
  }
}