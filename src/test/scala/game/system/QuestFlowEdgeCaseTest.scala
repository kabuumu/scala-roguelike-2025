package game.system

import org.scalatest.funsuite.AnyFunSuite
import game.GameState
import game.entity._
import game.entity.ConversationAction._
import game.quest.{
  QuestRepository,
  QuestStatus,
  RetrieveItemGoal,
  QuestRewards,
  Quest
}
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{
  CollisionEvent,
  CollisionTarget,
  AddExperienceEvent
}
import data.Items
import game.Point

class QuestFlowEdgeCaseTest extends AnyFunSuite {

  test(
    "Quest Flow: Pickup BEFORE Accept - Dialogue should update immediately upon accept if item held"
  ) {
    // 1. Setup
    val player = Entity(
      id = "player",
      Movement(Point(0, 0)),
      EntityTypeComponent(EntityType.Player),
      Inventory(Nil),
      Coins(0),
      Experience(0)
    )

    val elder = Entity(
      id = "elder",
      Movement(Point(1, 1)),
      NameComponent("Elder", "Quest Giver"),
      Conversation(
        "Help me!",
        Seq(ConversationChoice("I will", AcceptQuest("retrieve_statue")))
      )
    )

    val questItem =
      Items.goldenStatue("quest_item_1").addComponent(Movement(Point(2, 2)))

    val initialState = GameState(
      playerEntityId = player.id,
      entities = Vector(player, elder, questItem),
      worldMap = map.WorldMap(
        tiles = Map.empty,
        dungeons = Seq.empty,
        paths = Set.empty,
        bridges = Set.empty,
        bounds = map.MapBounds(0, 10, 0, 10)
      ),
      quests = Map.empty
    )

    // 2. Pickup Item (Before Quest Active)
    val pickupEvent =
      CollisionEvent(player.id, CollisionTarget.Entity(questItem.id))
    val (stateAfterPickup, _) =
      InventorySystem.update(initialState, Seq(pickupEvent))

    // Player has item
    assert(
      stateAfterPickup.playerEntity
        .get[Inventory]
        .exists(_.itemEntityIds.contains(questItem.id))
    )
    // Elder dialogue should NOT be updated yet (Quest not active)
    val elderChk1 =
      stateAfterPickup.getEntity(elder.id).get.get[Conversation].get
    assert(elderChk1.text == "Help me!")

    // 3. Accept Quest
    // Now we accept the quest. logic should ideally check if we already satisfy it?
    // OR allow us to turn it in immediately?
    val (stateAfterAccept, _) = ConversationSystem.handleConversationAction(
      stateAfterPickup,
      elder, // passing the elder entity here
      AcceptQuest("retrieve_statue")
    )

    assert(stateAfterAccept.isQuestActive("retrieve_statue"))

    // 4. Check Dialogue
    // Since we accepted and already have the item, does the system know to update the dialogue?
    // Or is the dialogue stuck on "Help me!" until some other event?
    val elderFinal = stateAfterAccept.getEntity(elder.id).get
    val elderConv = elderFinal.get[Conversation].get

    // This is where we expect failure if the bug exists
    assert(
      elderConv.text.contains("You found it"),
      s"Dialogue was: '${elderConv.text}', expected 'You found it'"
    )
  }
}
