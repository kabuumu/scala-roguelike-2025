package map

import game.{Direction, Point}

import scala.annotation.tailrec

object DungeonGenerator {

  /** Maximum iterations for dungeon generation before declaring configuration impossible */
  private val MaxGenerationIterations = 10000

  /**
   * Generates a dungeon with simplified bounds-only configuration.
   * All dungeon parameters (size, locked doors, items) are automatically calculated
   * based on the available space defined by bounds.
   *
   * @param bounds The rectangular bounds defining how much space the dungeon can occupy
   * @param seed   Random seed for reproducible generation
   * @return Generated Dungeon that fits within the bounds
   */
  def generateDungeon(bounds: MapBounds, seed: Long): Dungeon = {
    val config = DungeonConfig(bounds, seed)
    generateDungeon(config)
  }

  /**
   * Generates a dungeon with explicit configuration.
   *
   * @param config DungeonConfig specifying all dungeon parameters
   * @return Generated Dungeon
   */
  def generateDungeon(config: DungeonConfig): Dungeon = {
    val startTime = System.currentTimeMillis()

    val mutators: Set[DungeonMutator] = Set(
      new EndPointMutator(config.size),
      new TraderRoomMutator(config.size),
      new KeyLockMutator(config.lockedDoorCount, config.size),
      new TreasureRoomMutator(config.itemCount, config.size),
      new BossRoomMutator(config.size)
    )

    @tailrec
    def recursiveGenerator(openDungeons: Set[Dungeon], iterations: Int = 0): Dungeon = {
      if (openDungeons.isEmpty) {
        throw new IllegalStateException(
          s"Cannot generate dungeon: bounds too restrictive or configuration impossible. " +
            s"Bounds: ${config.bounds.describe}, size: ${config.size}, " +
            s"locked doors: ${config.lockedDoorCount}, items: ${config.itemCount}"
        )
      }

      if (iterations > MaxGenerationIterations) {
        throw new IllegalStateException(
          s"Dungeon generation exceeded maximum iterations ($MaxGenerationIterations). " +
            s"Configuration may be impossible to satisfy: ${config.bounds.describe}, size: ${config.size}"
        )
      }

      val currentDungeon: Dungeon = openDungeons.maxBy(dungeon =>
        dungeon.roomGrid.size + dungeon.lockedDoorCount + dungeon.nonKeyItems.size
      )

      // Generate mutations that respect bounds configuration
      val newOpenDungeons: Set[Dungeon] = for {
        mutator <- mutators
        possibleDungeon <- mutator.getPossibleMutations(currentDungeon)
        if possibleDungeon.roomGrid.forall(config.isWithinBounds)
      } yield possibleDungeon

      newOpenDungeons.find(dungeon =>
        dungeon.lockedDoorCount == config.lockedDoorCount
          && dungeon.nonKeyItems.size == config.itemCount
          && dungeon.roomGrid.size == config.size
          && dungeon.traderRoom.isDefined
          && dungeon.hasBossRoom
      ) match {
        case Some(completedDungeon) =>
          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon, iterations + 1)
      }
    }

    // Start from configured entrance room
    val startRoom = config.getEntranceRoom
    recursiveGenerator(Set(Dungeon(roomGrid = Set(startRoom), startPoint = startRoom, seed = config.seed, entranceSide = config.entranceSide)))
  }
}
