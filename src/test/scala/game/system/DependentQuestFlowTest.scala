package game.system

import game.{GameState, Point}
import game.entity.*
import game.entity.EventMemory.*
import game.quest.{QuestRepository, QuestState, QuestStatus}
import game.system.event.GameSystemEvent.AddExperienceEvent
import map.{MapBounds, WorldMap}
import org.scalatest.funsuite.AnyFunSuite

class DependentQuestFlowTest extends AnyFunSuite {

  private def ratKill(index: Int): MemoryEvent.EnemyDefeated =
    MemoryEvent.EnemyDefeated(
      timestamp = index.toLong,
      enemyType = "Rat",
      method = "combat"
    )

  private def addRatKills(entity: Entity, count: Int): Entity =
    (1 to count).foldLeft(entity)((current, index) =>
      current.addMemoryEvent(ratKill(index))
    )

  private def state(
      player: Entity,
      questStates: Map[String, QuestState] = Map.empty
  ): (GameState, Entity) = {
    val giver = Entity(
      "rat-giver",
      Movement(Point(1, 1)),
      EntityTypeComponent(EntityType.Villager),
      NameComponent("Quest Giver"),
      Conversation(
        "Rats have infested our storerooms!",
        Seq(
          ConversationChoice(
            "I will help.",
            ConversationAction.AcceptQuest("kill_rats")
          )
        )
      )
    )
    val gameState = GameState(
      playerEntityId = player.id,
      entities = Vector(player, giver),
      worldMap = WorldMap(
        tiles = Map.empty,
        dungeons = Seq.empty,
        paths = Set.empty,
        bridges = Set.empty,
        bounds = MapBounds(0, 1, 0, 1)
      ),
      quests = questStates
    )
    (gameState, giver)
  }

  private def player: Entity =
    Entity(
      "player",
      Movement(Point(0, 0)),
      EntityTypeComponent(EntityType.Player),
      Inventory(Seq.empty),
      EventMemory(),
      Coins(0),
      Experience(0)
    )

  test("cannot accept a quest until all prerequisites are completed") {
    val (initialState, giver) = state(player)

    val (result, events) =
      QuestSystem.acceptQuest(initialState, giver, "kill_rats")

    assert(!result.isQuestActive("kill_rats"))
    assert(result.messages.head.contains("The Missing Statue"))
    assert(events.isEmpty)
  }

  test("kill quest progress excludes kills recorded before acceptance") {
    val playerWithOldKills = addRatKills(player, 3)
    val (initialState, giver) = state(
      playerWithOldKills,
      Map("retrieve_statue" -> QuestState(QuestStatus.Completed))
    )

    val (accepted, _) =
      QuestSystem.acceptQuest(initialState, giver, "kill_rats")

    assert(accepted.quests("kill_rats").progressBaseline == 3)
    assert(QuestSystem.progress(accepted, "kill_rats") == 0)
    assert(!QuestSystem.isGoalSatisfied(accepted, "kill_rats"))
  }

  test("completing a kill quest grants rewards exactly once") {
    val (initialState, giver) = state(
      player,
      Map("retrieve_statue" -> QuestState(QuestStatus.Completed))
    )
    val (accepted, _) =
      QuestSystem.acceptQuest(initialState, giver, "kill_rats")
    val playerWithKills = addRatKills(accepted.playerEntity, 3)
    val ready = accepted.updateEntity(player.id, playerWithKills)

    val (completed, events) =
      QuestSystem.completeQuest(ready, giver, "kill_rats")

    assert(completed.isQuestCompleted("kill_rats"))
    assert(completed.playerEntity.get[Coins].exists(_.current == 50))
    assert(
      events.count {
        case AddExperienceEvent("player", 300) => true
        case _                                 => false
      } == 1
    )

    val (repeated, repeatedEvents) =
      QuestSystem.completeQuest(completed, giver, "kill_rats")

    assert(repeated == completed)
    assert(repeatedEvents.isEmpty)
  }

  test("quest repository defines a consistent four-quest chain") {
    val retrieve = QuestRepository.get("retrieve_statue").get
    val rats = QuestRepository.get("kill_rats").get
    val slimes = QuestRepository.get("slime_cleanup").get
    val snakes = QuestRepository.get("snake_hunt").get

    assert(retrieve.followUpQuestId.contains("kill_rats"))
    assert(rats.prerequisiteQuestIds == Set("retrieve_statue"))
    assert(rats.followUpQuestId.contains("slime_cleanup"))
    assert(slimes.prerequisiteQuestIds == Set("kill_rats"))
    assert(slimes.followUpQuestId.contains("snake_hunt"))
    assert(snakes.prerequisiteQuestIds == Set("slime_cleanup"))
    assert(snakes.followUpQuestId.isEmpty)
  }

  test("completing a quest offers its same-giver follow-up") {
    val (initialState, giver) = state(
      player,
      Map("retrieve_statue" -> QuestState(QuestStatus.Completed))
    )
    val (accepted, _) =
      QuestSystem.acceptQuest(initialState, giver, "kill_rats")
    val ready = accepted.updateEntity(
      player.id,
      addRatKills(accepted.playerEntity, 3)
    )

    val (completed, _) =
      QuestSystem.completeQuest(ready, giver, "kill_rats")
    val conversation =
      completed.getEntity(giver.id).flatMap(_.get[Conversation]).get

    assert(
      conversation.choices.exists(
        _.action == ConversationAction.AcceptQuest("slime_cleanup")
      )
    )
  }
}
