package map

import game.{Direction, Point}

import scala.annotation.tailrec

object DungeonGenerator {

  /** Maximum iterations for dungeon generation before declaring configuration
    * impossible
    */
  private val MaxGenerationIterations = 10000

  /** Generates a dungeon with simplified bounds-only configuration. All dungeon
    * parameters (size, locked doors, items) are automatically calculated based
    * on the available space defined by bounds.
    *
    * @param bounds
    *   The rectangular bounds defining how much space the dungeon can occupy
    * @param seed
    *   Random seed for reproducible generation
    * @return
    *   Generated Dungeon that fits within the bounds
    */
  def generateDungeon(bounds: MapBounds, seed: Long): Dungeon = {
    val config = DungeonConfig(bounds, seed)
    generateDungeon(config)
  }

  /** Generates a dungeon with explicit configuration.
    *
    * @param config
    *   DungeonConfig specifying all dungeon parameters
    * @return
    *   Generated Dungeon
    */
  def generateDungeon(config: DungeonConfig): Dungeon = {
    val startTime = System.currentTimeMillis()

    // Use Seq to ensure deterministic order of mutation application
    val mutators: Seq[DungeonMutator] = Seq(
      new EndPointMutator(config.size),
      new TraderRoomMutator(config.size),
      new KeyLockMutator(config.lockedDoorCount, config.size),
      new TreasureRoomMutator(config.itemCount, config.size),
      new BossRoomMutator(config.size)
    )

    // Wrapper to cache score and ensure deterministic ordering
    case class Candidate(dungeon: Dungeon) {
      val score: Int =
        dungeon.roomGrid.size + dungeon.lockedDoorCount + dungeon.nonKeyItems.size
      // Cache structural hash to avoid expensive recomputation (ignoring tiles/noise)
      val tieBreaker: Int =
        dungeon.roomGrid.hashCode() ^ dungeon.roomConnections.hashCode()

      // Deterministic hash independent of Set iteration order
      // Sort points to ensure canonical order, then compute hash
      val topoHash: Int = dungeon.roomGrid.toSeq
        .sortBy(p => (p.x, p.y))
        .foldLeft(0)((acc, p) => acc * 31 + p.hashCode())
    }

    // Ordering for candidates
    implicit val candidateOrdering: Ordering[Candidate] =
      Ordering.by(c => (c.score, c.tieBreaker, c.topoHash))

    @tailrec
    def recursiveGenerator(
        openCandidates: Set[Candidate],
        iterations: Int = 0
    ): Dungeon = {
      if (openCandidates.isEmpty) {
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

      // Beam Search optimization:
      // If we have too many candidates, prune the search space to keep only the best ones.
      // This prevents memory explosion and keeps iterations fast.
      val effectiveCandidates = if (openCandidates.size > 200) {
        // Convert to Seq, sort descending, take top 50, convert back to Set
        openCandidates.toSeq.sorted(candidateOrdering.reverse).take(50).toSet
      } else {
        openCandidates
      }

      // Deterministic selection using precomputed ordering
      val bestCandidate = effectiveCandidates.max(candidateOrdering)
      val currentDungeon = bestCandidate.dungeon

      // Generate mutations that respect bounds configuration
      val newCandidates: Set[Candidate] = (for {
        mutator <- mutators
        possibleDungeon <- mutator.getPossibleMutations(currentDungeon)
        if possibleDungeon.roomGrid.forall(config.isWithinBounds)
      } yield Candidate(possibleDungeon)).toSet

      // Check if we found a solution
      // We sort the new candidates deterministically before checking "find"
      // to ensure we pick the same solution if multiple are valid within this batch.
      newCandidates.toSeq
        .sorted(candidateOrdering.reverse)
        .find(c =>
          c.dungeon.lockedDoorCount == config.lockedDoorCount
            && c.dungeon.nonKeyItems.size == config.itemCount
            && c.dungeon.roomGrid.size == config.size
            && c.dungeon.traderRoom.isDefined
            && c.dungeon.hasBossRoom
        ) match {
        case Some(completedCandidate) =>
          completedCandidate.dungeon
        case None =>
          recursiveGenerator(
            newCandidates ++ effectiveCandidates - bestCandidate,
            iterations + 1
          )
      }
    }

    // Start from configured entrance room
    val startRoom = config.getEntranceRoom
    recursiveGenerator(
      Set(
        Candidate(
          Dungeon(
            roomGrid = Set(startRoom),
            startPoint = startRoom,
            seed = config.seed,
            entranceSide = config.entranceSide
          )
        )
      )
    )
  }
}
