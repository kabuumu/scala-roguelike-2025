package game.system

import game.GameState
import game.entity.Entity
import game.entity.ConversationAction.*
import game.entity.Coins.{addCoins, removeCoins, coins}
import game.system.event.GameSystemEvent.{
  GameSystemEvent,
  InputEvent,
  HealEvent
}
import ui.InputAction
import game.entity.Health.*
import game.entity.Inventory.{inventoryItems, removeItemEntity}
import game.quest.{QuestRepository, RetrieveItemGoal}

object ConversationSystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val inputEvents = events.collect { case e: InputEvent => e }

    val (updatedGameState, newEvents) =
      inputEvents.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
        case ((state, currentEvents), event) =>
          event.input match {
            case InputAction.ConversationAction(entity, action) =>
              val (newState, actionEvents) =
                handleConversationAction(state, entity, action)
              (newState, currentEvents ++ actionEvents)
            case _ => (state, currentEvents)
          }
      }

    (updatedGameState, newEvents)
  }

  private[game] def handleConversationAction(
      gameState: GameState,
      entity: Entity,
      action: game.entity.ConversationAction
  ): (GameState, Seq[GameSystemEvent]) = {
    action match {
      case HealAction(amount, cost) =>
        if (gameState.playerEntity.coins >= cost) {
          val player = gameState.playerEntity
          val updatedPlayer = player.removeCoins(cost)
          val updatedEntities = gameState.entities
            .filterNot(_.id == player.id) :+ updatedPlayer

          val newState = gameState
            .copy(entities = updatedEntities)
            .addMessage(s"You paid $cost coins for healing.")

          (newState, Seq(HealEvent(player.id, amount)))
        } else {
          (
            gameState.addMessage(s"You don't have enough coins (Need $cost)."),
            Seq.empty
          )
        }
      case AcceptQuest(questId) =>
        if (
          !gameState
            .isQuestActive(questId) && !gameState.isQuestCompleted(questId)
        ) {
          QuestRepository.get(questId) match {
            case Some(quest) =>
              val stateWithQuest = gameState
                .acceptQuest(questId)
                .addMessage(s"Accepted quest: ${quest.title}")

              // Check if player already has the item for this quest
              val updatedState = quest.goal match {
                case RetrieveItemGoal(itemRef, amount) =>
                  if (QuestSystem.hasQuestItem(gameState, itemRef, amount)) {
                    // Update entity dialogue immediately
                    val updatedNpc = entity.update[game.entity.Conversation] {
                      _ =>
                        game.entity.Conversation(
                          "You found it! Please, give it to me.",
                          Seq(
                            game.entity.ConversationChoice(
                              "Give Statue",
                              CompleteQuest(questId)
                            ),
                            game.entity.ConversationChoice(
                              "Not yet",
                              CloseAction
                            )
                          )
                        )
                    }
                    stateWithQuest
                      .updateEntity(entity.id, updatedNpc)
                      .addMessage("You already have the item!")
                  } else {
                    stateWithQuest
                  }
                case _ => stateWithQuest
              }

              (updatedState, Seq.empty)
            case None => (gameState, Seq.empty)
          }
        } else {
          (gameState, Seq.empty)
        }

      case CompleteQuest(questId) =>
        if (gameState.isQuestActive(questId)) {
          QuestRepository.get(questId) match {
            case Some(quest) =>
              quest.goal match {
                case RetrieveItemGoal(itemRef, amount) =>
                  if (QuestSystem.hasQuestItem(gameState, itemRef, amount)) {
                    val player = gameState.playerEntity
                    val inventoryItems = player.inventoryItems(gameState)

                    // Specific logic for removal still needs to find the items, but we can assume they exist/names match
                    val foundItems = inventoryItems.filter { item =>
                      item
                        .get[game.entity.NameComponent]
                        .exists(_.name == itemRef.name)
                    }

                    val itemsToRemove = foundItems.take(amount)
                    val itemsToRemoveIds = itemsToRemove.map(_.id).toSet

                    // 1. Remove quest items from world/inventory
                    val entitiesAfterItemRemoval = gameState.entities.filterNot(
                      e => itemsToRemoveIds.contains(e.id)
                    )

                    // 2. Add Coins Reward and Remove Items from Inventory
                    var updatedPlayer = player.addCoins(quest.rewards.coins)
                    itemsToRemoveIds.foreach { id =>
                      updatedPlayer = updatedPlayer.removeItemEntity(id)
                    }

                    // 3. Update NPC Dialogue (Post-Quest)
                    val updatedNpc = entity.update[game.entity.Conversation] {
                      _ =>
                        game.entity.Conversation(
                          "Thank you so much! Our village is safe.",
                          Seq(
                            game.entity
                              .ConversationChoice("Goodbye", CloseAction)
                          )
                        )
                    }

                    // 4. Update GameState
                    val newState = gameState
                      .copy(entities = entitiesAfterItemRemoval)
                      .updateEntity(player.id, updatedPlayer)
                      .updateEntity(entity.id, updatedNpc)
                      .completeQuest(questId)
                      .addMessage(s"Completed quest: ${quest.title}!")
                      .addMessage(
                        s"Received ${quest.rewards.coins} coins and ${quest.rewards.experience} XP!"
                      )

                    // 5. Emit Experience Event
                    (
                      newState,
                      Seq(
                        game.system.event.GameSystemEvent.AddExperienceEvent(
                          player.id,
                          quest.rewards.experience
                        )
                      )
                    )
                  } else {
                    (
                      gameState.addMessage("You don't have the required item."),
                      Seq.empty
                    )
                  }
                case _ => (gameState, Seq.empty)
              }
            case None => (gameState, Seq.empty)
          }
        } else {
          (gameState, Seq.empty)
        }

      case _ => (gameState, Seq.empty)
    }

  }
}
