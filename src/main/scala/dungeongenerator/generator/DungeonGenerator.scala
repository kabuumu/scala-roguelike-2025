package dungeongenerator.generator

import dungeongenerator.generator.predicates.DungeonPredicate

import scala.annotation.tailrec

object DungeonGenerator {

  @tailrec
  def generatePossibleDungeonsLinear(completedDungeons: Iterable[Dungeon] = Set.empty,
                                     openDungeons: Iterable[Dungeon] = Set(Dungeon.empty),
                                     config: DungeonGeneratorConfig): Iterable[Dungeon] = {

    def getPredicateScore(dungeon: Dungeon): Either[String, Double] = {
      @tailrec
      def iterateDungeonScore(predicates: Iterable[DungeonPredicate], currentScore: Double): Either[String, Double] = {
        predicates.toList match {
          case predicate :: remainingPredicates =>
            predicate.dungeonScore(dungeon) match {
              case Right(newScore) => iterateDungeonScore(remainingPredicates, currentScore + newScore)
              case Left(predicateFailure) => Left(predicateFailure)
            }
          case Nil =>
            Right(currentScore)
        }
      }

      iterateDungeonScore(config.predicates, 0)
    }


    if (openDungeons.isEmpty || completedDungeons.size >= config.targetCount) {
      completedDungeons.take(config.targetCount)
    } else {
      val currentDungeon = openDungeons.toSeq.maxBy{
        dungeon =>
          val predicateScore = getPredicateScore(dungeon)
          predicateScore match {
            case Right(score) => score
            case Left(_) => 0
          }
      }
      val currentPredicateScore = getPredicateScore(currentDungeon)
      println(s"New iteration. Dungeon Score: $currentPredicateScore")

      val possibleMutations: Iterable[Dungeon] = for {
        mutator <- config.mutators
        newDungeon <- mutator.getPossibleMutations(currentDungeon, config)
        newPredicateScore = getPredicateScore(newDungeon)
        if newDungeon.longestRoomPath.nonEmpty
        _ = {
          println(s"  For $mutator. Dungeon Score: $newPredicateScore")
          config.predicates.foreach { predicate =>
            val predicateScore = predicate.dungeonScore(newDungeon)
            println(s"    $predicate:$predicateScore")
          }
        }
      } yield newDungeon

      val (newCompletedDungeons, newOpenDungeons) = possibleMutations.partition(
        dungeon => config.predicates.forall(
          predicate => predicate.dungeonScore(dungeon) match {
            case Right(dungeonScore) => dungeonScore == 1
            case Left(_) => false
          }
        )
      )

      generatePossibleDungeonsLinear(
        completedDungeons = completedDungeons ++ newCompletedDungeons,
        openDungeons = (openDungeons ++ newOpenDungeons).filterNot(_ == currentDungeon),
        config
      )
    }
  }
}
