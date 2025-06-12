package game.system

import game.GameState
import game.system.event.GameSystemEvent
import ui.InputAction

object ItemUseSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedGameState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.UseItem(itemEffect))) =>
        currentState.getEntity(entityId) match {
          case Some(entity) =>
            currentState.handleEvents(itemEffect.apply(entity)(currentState))
          case _ =>
            // If the entity does not exist
            currentState
        }
      case (currentState, _) =>
        currentState
    }

    (updatedGameState, Nil)
}
