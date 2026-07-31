package game.system

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import game.GameState
import game.entity.{
  Entity,
  EntityType,
  EntityTypeComponent,
  Movement,
  Health,
  Initiative,
  Inventory,
  SightMemory,
  Equipment,
  EventMemory,
  MemoryEvent,
  NameComponent,
  Conversation,
  ConversationChoice,
  ConversationAction
}
import game.entity.EventMemory.*
import game.quest.{QuestStatus, QuestRepository, Quest, KillEnemyGoal, QuestRewards}
import game.Point
import map.{WorldMap, MapBounds}
import game.GameMode
import game.system.event.GameSystemEvent

class QuestSystemRatQuestTest extends AnyFunSuite with Matchers {

  test("QuestSystem handles KillEnemyGoal correctly") {
    // 1. Setup mock Player
    val playerEntity = Entity(
      id = "PlayerID",
      Movement(Point(0, 0)),
      EntityTypeComponent(EntityType.Player),
      Health(10),
      Initiative(10),
      Inventory(Seq.empty),
      Equipment(None, None),
      SightMemory(),
      EventMemory()
    )

    // 2. Setup mock QuestGiver
    val initialConversation = Conversation(
      "Please kill some rats for me.",
      Seq(ConversationChoice("Will do.", ConversationAction.CloseAction))
    )

    val questGiver = Entity(
      id = "QuestGiverID",
      Movement(Point(1, 1)),
      EntityTypeComponent(EntityType.Player), // Just a dummy entity, let's use Player as a fallback or if there is a Villager type I'll update it later
      NameComponent("Quest Giver"),
      Health(10),
      initialConversation
    )

    // 3. Setup GameState
    val worldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = MapBounds(0, 10, 0, 10)
    )

    val gameState = GameState(
      playerEntityId = playerEntity.id,
      entities = Vector(playerEntity, questGiver),
      worldMap = worldMap,
      dungeonFloor = 0,
      gameMode = GameMode.Adventure,
      quests = Map("kill_rats" -> QuestStatus.Active)
    )

    // Retrieve active quest from state
    assert(gameState.quests("kill_rats") == QuestStatus.Active)

    // 4. Update QuestSystem without rat kills (Should not complete)
    val (state1, events1) = QuestSystem.update(gameState, Seq.empty)
    val npcConversation1 = state1.getEntity("QuestGiverID").get.get[Conversation].get
    assert(npcConversation1.text == "Please kill some rats for me.")

    // 5. Simulate killing 3 rats (add to EventMemory)
    val killEvent = MemoryEvent.EnemyDefeated(
      timestamp = System.nanoTime(),
      enemyType = "Rat",
      method = "combat"
    )

    // Add 3 kills
    val playerWithKills = playerEntity
      .addMemoryEvent(killEvent)
      .addMemoryEvent(killEvent)
      .addMemoryEvent(killEvent)

    val stateWithKills = state1.updateEntity(playerEntity.id, playerWithKills)

    // 6. Update QuestSystem WITH rat kills (Should complete)
    val (state2, events2) = QuestSystem.update(stateWithKills, Seq.empty)
    val npcConversation2 = state2.getEntity("QuestGiverID").get.get[Conversation].get
    
    // NPC dialogue should be ready to complete
    assert(npcConversation2.text == "You killed them? Thank you!")
    assert(npcConversation2.choices.exists(_.action == ConversationAction.CompleteQuest("kill_rats")))
  }
}
