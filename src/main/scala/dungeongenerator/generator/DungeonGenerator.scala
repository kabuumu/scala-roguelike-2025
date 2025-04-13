package dungeongenerator.generator

import scala.annotation.tailrec

object DungeonGenerator {

  @tailrec
  def generatePossibleDungeons(completedDungeons: Set[Dungeon] = Set.empty,
                               openDungeons: Set[Dungeon] = Set(Dungeon.empty),
                               config: DungeonGeneratorConfig): Set[Dungeon] = {

    def getPredicateScore(dungeon: Dungeon): Double =
      config.predicates.view.map(_.dungeonScore(dungeon).getOrElse(0.0)).sum

    if (openDungeons.isEmpty || completedDungeons.size >= config.targetCount) {
      completedDungeons.take(config.targetCount)
    } else {
      val currentDungeon = openDungeons.maxBy(getPredicateScore)

      val mutations = config.mutators
        .flatMap(_.getPossibleMutations(currentDungeon, config))
        .filter(_.longestRoomPath.nonEmpty)

      val (newCompleted, newOpen) = mutations.partition(dungeon =>
        config.predicates.forall(_.dungeonScore(dungeon).contains(1.0))
      )

      generatePossibleDungeons(
        completedDungeons ++ newCompleted,
        openDungeons ++ newOpen - currentDungeon,
        config
      )
    }
  }
}