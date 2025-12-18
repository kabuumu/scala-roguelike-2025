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

  private def handleConversationAction(
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
      case _ => (gameState, Seq.empty)
    }
  }
}
