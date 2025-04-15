package dungeongenerator.generator

import scala.annotation.tailrec
import scala.collection.parallel.CollectionConverters.*


object DungeonGenerator {

  @tailrec
  def generatePossibleDungeons(completedDungeons: Set[Dungeon] = Set.empty,
                               openDungeons: Set[Dungeon] = Set(Dungeon.empty),
                               config: DungeonGeneratorConfig): Set[Dungeon] = {

    def getPredicateScore(dungeon: Dungeon): Double =
      config.predicates.view.map(_.dungeonScore(dungeon).getOrElse(-10.0)).sum

    if (openDungeons.isEmpty || completedDungeons.size >= config.targetCount) {
      completedDungeons.take(config.targetCount)
    } else {

//      val currentDungeon = openDungeons.maxBy(getPredicateScore)

      val maxScore: Double = openDungeons.map(getPredicateScore).max

      val currentDungeons = openDungeons.toSeq.sortBy(getPredicateScore).reverse.take(3).par

      val mutations = for {
        dungeon <- currentDungeons
        mutator <- config.mutators
        possibleMutation <- mutator.getPossibleMutations(dungeon, config)
        if possibleMutation.longestRoomPath.nonEmpty
      } yield possibleMutation

      val (newCompleted, newOpen) = mutations.partition(dungeon =>
        config.predicates.forall(_.dungeonScore(dungeon).contains(1.0))
      )

      generatePossibleDungeons(
        completedDungeons ++ newCompleted,
        openDungeons ++ newOpen -- currentDungeons,
        config
      )
    }
  }
}