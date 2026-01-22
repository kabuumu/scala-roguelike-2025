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

class QuestFlowTest extends AnyFunSuite {

  test("Quest Flow: Accept -> Pickup -> Complete") {
    // 1. Setup
    // Create Player
    val player = Entity(
      id = "player",
      Movement(Point(0, 0)),
      EntityTypeComponent(EntityType.Player),
      Inventory(Nil),
      Coins(0),
      Experience(0)
    )

    // Create Elder with Quest
    val elder = Entity(
      id = "elder",
      Movement(Point(1, 1)),
      NameComponent("Elder", "Quest Giver"),
      Conversation(
        "Help me!",
        Seq(ConversationChoice("I will", AcceptQuest("retrieve_statue")))
      )
    )

    // Create Quest Item
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

    // 2. Accept Quest
    // Simulate accepting quest (as ConversationSystem does that part simply)
    val stateWithQuest = initialState.acceptQuest("retrieve_statue")
    assert(stateWithQuest.isQuestActive("retrieve_statue"))

    // 3. Pickup Item
    val pickupEvent =
      CollisionEvent(player.id, CollisionTarget.Entity(questItem.id))
    val (stateAfterInventory, eventsAfterInventory) =
      InventorySystem.update(stateWithQuest, Seq(pickupEvent))

    // Run QuestSystem to update dialogue
    val (stateAfterPickup, eventsAfterQuest) =
      QuestSystem.update(stateAfterInventory, eventsAfterInventory)

    // Check Player has item
    val playerAfterPickup = stateAfterPickup.playerEntity
    assert(
      playerAfterPickup
        .get[Inventory]
        .exists(_.itemEntityIds.contains(questItem.id))
    )

    // Check Elder dialogue updated
    val elderAfterPickup = stateAfterPickup.getEntity(elder.id).get
    val elderConv = elderAfterPickup.get[Conversation].get
    assert(elderConv.text.contains("You found it"))
    assert(elderConv.choices.exists {
      case ConversationChoice(_, CompleteQuest("retrieve_statue")) => true
      case _                                                       => false
    })

    // 4. Complete Quest
    // Trigger Conversation logic for CompleteQuest
    val (stateAfterCompletion, eventsAfterCompletion) =
      ConversationSystem.handleConversationAction(
        stateAfterPickup,
        elderAfterPickup,
        CompleteQuest("retrieve_statue")
      )

    // Check Quest Completed
    assert(stateAfterCompletion.isQuestCompleted("retrieve_statue"))

    // Check Rewards
    val playerAfterCompletion = stateAfterCompletion.playerEntity
    assert(
      playerAfterCompletion.get[Coins].get.current == 100
    ) // 100 coins reward

    // Check Events (XP)
    assert(eventsAfterCompletion.exists {
      case AddExperienceEvent(pid, amount) => pid == player.id && amount == 500
      case _                               => false
    })

    // Check Item Removed
    assert(
      !playerAfterCompletion
        .get[Inventory]
        .exists(_.itemEntityIds.contains(questItem.id))
    )

    // Check Elder Dialogue Final
    val elderFinal = stateAfterCompletion.getEntity(elder.id).get
    assert(elderFinal.get[Conversation].get.text.contains("Thank you"))
  }
}
