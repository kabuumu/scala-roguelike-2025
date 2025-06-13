package game.system

import game.GameState
import game.system.event.GameSystemEvent
import ui.InputAction
import game.entity.Initiative.*

object WaitSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.Wait)) =>
        currentState.getEntity(entityId) match {
          case Some(entity) if entity.isReady =>
            currentState.updateEntity(entityId, _.resetInitiative())
          case _ =>
            // If the entity does not exist or is not ready, do nothing
            currentState
        }
      case (currentState, _) =>
        currentState
    }

    (updatedState, Nil)
}
