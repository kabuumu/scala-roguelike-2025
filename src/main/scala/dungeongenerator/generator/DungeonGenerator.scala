package dungeongenerator.generator

import scala.annotation.tailrec

object DungeonGenerator {

  @tailrec
  def generatePossibleDungeonsLinear(completedDungeons: Iterable[Dungeon] = Set.empty,
                                     openDungeons: Iterable[Dungeon] = Set(Dungeon.empty),
                                     config: DungeonGeneratorConfig): Iterable[Dungeon] = {
    def getPredicateScore(dungeon: Dungeon): Double = {
      val score = config.predicates.foldLeft(0.0) { (score, predicate) =>
        println(s"Predicate: ${predicate.getClass.getSimpleName}, Score: ${predicate.dungeonScore(dungeon)}")

        predicate.dungeonScore(dungeon).getOrElse(0.0) + score
      }

      println(s"Total Score: ${score/config.predicates.size}")
      score
    }

    if (openDungeons.isEmpty || completedDungeons.size >= config.targetCount) {
      completedDungeons.take(config.targetCount)
    } else {
      val currentDungeon = openDungeons.maxBy(getPredicateScore)

      val (newCompletedDungeons, newOpenDungeons) = config.mutators
        .flatMap(_.getPossibleMutations(currentDungeon, config))
        .filter(_.longestRoomPath.nonEmpty)
        .partition(dungeon => config.predicates.forall(_.dungeonScore(dungeon).contains(1.0)))

      generatePossibleDungeonsLinear(
        completedDungeons ++ newCompletedDungeons,
        (openDungeons ++ newOpenDungeons).filterNot(_ == currentDungeon),
        config
      )
    }
  }
}