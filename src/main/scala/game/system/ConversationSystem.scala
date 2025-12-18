package game.system

import game.GameState
import game.entity.Entity
import game.entity.ConversationAction.*
import game.entity.Coins.{addCoins, removeCoins, coins}
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import ui.InputAction
import game.entity.Health.*

object ConversationSystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val inputEvents = events.collect { case e: InputEvent => e }

    val updatedGameState = inputEvents.foldLeft(gameState) { (state, event) =>
      event.input match {
        case InputAction.ConversationAction(entity, action) =>
          handleConversationAction(state, entity, action)
        case _ => state
      }
    }

    (updatedGameState, Seq.empty)
  }

  private def handleConversationAction(
      gameState: GameState,
      entity: Entity,
      action: game.entity.ConversationAction
  ): GameState = {
    action match {
      case HealAction(amount, cost) =>
        if (gameState.playerEntity.coins >= cost) {
          val player = gameState.playerEntity
          val healedPlayer = player.heal(amount).removeCoins(cost)
          val updatedEntities = gameState.entities
            .filterNot(_.id == player.id) :+ healedPlayer

          gameState
            .copy(entities = updatedEntities)
            .addMessage(s"You paid $cost coins and were healed for $amount HP.")
        } else {
          gameState.addMessage(s"You don't have enough coins (Need $cost).")
        }
      case _ => gameState
    }
  }
}
