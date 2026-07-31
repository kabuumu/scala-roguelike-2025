package game.quest

import data.Items.ItemReference
import game.{GameState, Point}
import game.entity.*
import map.{MapBounds, WorldMap}
import org.scalatest.funsuite.AnyFunSuite

class QuestRepositoryTest extends AnyFunSuite {

  private val rewards = QuestRewards(experience = 10, coins = 5)

  private def quest(
      id: String,
      goal: QuestGoal = KillEnemyGoal("Rat"),
      prerequisites: Set[String] = Set.empty,
      followUp: Option[String] = None
  ): Quest =
    Quest(
      id = id,
      title = id,
      description = s"$id description",
      goal = goal,
      rewards = rewards,
      prerequisiteQuestIds = prerequisites,
      followUpQuestId = followUp
    )

  test("validates a consistent dependent quest chain") {
    val quests = Map(
      "first" -> quest("first", followUp = Some("second")),
      "second" -> quest("second", prerequisites = Set("first"))
    )

    assert(QuestRepository.validate(quests) == Right(()))
  }

  test("rejects missing prerequisite quest references") {
    val quests = Map(
      "quest" -> quest("quest", prerequisites = Set("missing"))
    )

    assert(
      QuestRepository
        .validate(quests)
        .left
        .exists(_.contains("missing prerequisite"))
    )
  }

  test("rejects cyclic quest dependencies") {
    val quests = Map(
      "first" -> quest("first", prerequisites = Set("second")),
      "second" -> quest("second", prerequisites = Set("first"))
    )

    assert(
      QuestRepository.validate(quests).left.exists(_.contains("cycle"))
    )
  }

  test("rejects non-positive goal amounts") {
    val quests = Map(
      "item" -> quest(
        "item",
        goal = RetrieveItemGoal(ItemReference.GoldenStatue, amount = 0)
      )
    )

    assert(
      QuestRepository.validate(quests).left.exists(_.contains("positive"))
    )
  }

  test("GameState stores the progress baseline when accepting a quest") {
    val player = Entity(
      "player",
      Movement(Point(0, 0)),
      EntityTypeComponent(EntityType.Player)
    )
    val state = GameState(
      playerEntityId = player.id,
      entities = Vector(player),
      worldMap = WorldMap(
        tiles = Map.empty,
        dungeons = Seq.empty,
        paths = Set.empty,
        bridges = Set.empty,
        bounds = MapBounds(0, 1, 0, 1)
      )
    )

    val accepted = state.acceptQuest("kill_rats", progressBaseline = 4)

    assert(
      accepted.quests("kill_rats") ==
        QuestState(QuestStatus.Active, progressBaseline = 4)
    )
  }
}
