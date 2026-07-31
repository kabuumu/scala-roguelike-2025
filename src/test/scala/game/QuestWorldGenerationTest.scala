package game

import data.Enemies.EnemyReference
import data.Items.ItemReference
import game.entity.{EnemyTypeComponent, Movement, NameComponent}
import org.scalatest.funsuite.AnyFunSuite

class QuestWorldGenerationTest extends AnyFunSuite {

  private def namedPositions(
      state: GameState,
      name: String
  ): Seq[Point] =
    state.entities
      .filter(_.get[NameComponent].exists(_.name == name))
      .flatMap(_.get[Movement].map(_.position))

  private def enemyCount(
      state: GameState,
      enemyReference: EnemyReference
  ): Int =
    state.entities.count(
      _.get[EnemyTypeComponent].exists(_.enemyType == enemyReference)
    )

  test("all quest content is available across 25 world seeds") {
    val failures = (1L to 25L).flatMap { seed =>
      val state = StartingState.startAdventure(seed)
      val checks = Seq(
        (namedPositions(state, "Elder").size == 1) ->
          "exactly one Elder",
        (namedPositions(state, "Quest Giver").size == 1) ->
          "exactly one Quest Giver",
        state.worldMap.allItems.exists(_._2 == ItemReference.GoldenStatue) ->
          "a Golden Statue",
        (enemyCount(state, EnemyReference.Rat) >= 3) ->
          "at least 3 rats",
        (enemyCount(state, EnemyReference.Slimelet) >= 3) ->
          "at least 3 slimelets",
        (enemyCount(state, EnemyReference.Snake) >= 2) ->
          "at least 2 snakes"
      )

      checks.collect {
        case (false, requirement) =>
          s"seed $seed did not generate $requirement"
      }
    }

    assert(failures.isEmpty, failures.mkString("\n"))
  }

  test("quest content is deterministic for identical seeds") {
    Seq(1L, 7L, 13L, 19L, 25L).foreach { seed =>
      val first = StartingState.startAdventure(seed)
      val second = StartingState.startAdventure(seed)

      def questContent(state: GameState) =
        (
          namedPositions(state, "Elder"),
          namedPositions(state, "Quest Giver"),
          state.worldMap.allItems.toSeq.sortBy(entry =>
            (entry._1.x, entry._1.y, entry._2.toString)
          ),
          state.entities.flatMap(entity =>
            for {
              enemyType <- entity.get[EnemyTypeComponent]
              movement <- entity.get[Movement]
            } yield (
              enemyType.enemyType.toString,
              movement.position.x,
              movement.position.y
            )
          ).sorted
        )

      assert(questContent(first) == questContent(second), s"seed $seed")
    }
  }
}
