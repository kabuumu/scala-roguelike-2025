package game.system

import game.GameState
import game.system.event.GameSystemEvent.{GameSystemEvent, CollisionEvent}
import game.quest.{QuestRepository, QuestStatus, RetrieveItemGoal}
import game.entity.{
  Conversation,
  ConversationAction,
  ConversationChoice,
  NameComponent
}
import game.entity.Inventory.inventoryItems

object QuestSystem extends GameSystem {

  def hasQuestItem(
      gameState: GameState,
      itemRef: data.Items.ItemReference,
      amount: Int
  ): Boolean = {
    val player = gameState.playerEntity
    val inventoryItems = player.inventoryItems(gameState)
    inventoryItems.count { item =>
      item
        .get[NameComponent]
        .exists(_.name == itemRef.name) // Use localized name directly
    } >= amount
  }

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {

    // Process Active Quests
    val (finalState, finalEvents) =
      gameState.quests.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
        case ((currentState, currentEvents), (questId, QuestStatus.Active)) =>
          QuestRepository.get(questId) match {
            case Some(quest) =>
              quest.goal match {
                case RetrieveItemGoal(itemRef, amount) =>
                  if (hasQuestItem(currentState, itemRef, amount)) {
                    // Goal Satisfied: Check if NPC needs updating
                    quest.giverName match {
                      case Some(giverName) =>
                        currentState.entities.find(e =>
                          e.get[NameComponent].exists(_.name == giverName)
                        ) match {
                          case Some(npc) =>
                            val readyText = quest.readyToCompleteText.getOrElse(
                              "You found it! Please, give it to me."
                            )

                            // Check if dialogue is ALREADY updated to avoid spamming messages
                            val currentDialogue = npc.get[Conversation]
                            val needsUpdate = currentDialogue.exists(
                              _.text != readyText
                            )

                            if (needsUpdate) {
                              val updatedNpc = npc.update[Conversation] { _ =>
                                Conversation(
                                  readyText,
                                  Seq(
                                    ConversationChoice(
                                      "Give Item", // Generic text, could also be in Quest if needed
                                      ConversationAction.CompleteQuest(quest.id)
                                    ),
                                    ConversationChoice(
                                      "Not yet",
                                      ConversationAction.CloseAction
                                    )
                                  )
                                )
                              }

                              (
                                currentState
                                  .updateEntity(npc.id, updatedNpc)
                                  .addMessage(
                                    "Quest Updated: Return to the quest giver!"
                                  ),
                                currentEvents
                              )
                            } else {
                              (currentState, currentEvents)
                            }
                          case None => (currentState, currentEvents)
                        }
                      case None => (currentState, currentEvents)
                    }
                  } else {
                    // Goal NOT Satisfied (maybe dropped item?):
                    (currentState, currentEvents)
                  }

                case _ => (currentState, currentEvents)
              }
            case None => (currentState, currentEvents)
          }
        case ((currentState, currentEvents), _) => (currentState, currentEvents)
      }

    (finalState, finalEvents)
  }
}
