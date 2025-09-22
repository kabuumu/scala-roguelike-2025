package game.system

import game.GameState
import game.entity.Hitbox.*
import game.entity.Initiative.*
import game.entity.Movement
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
import ui.InputAction

object MovementSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGamestate = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.Move(direction))) =>
        currentState.getEntity(entityId) match {
          case Some(entity) if entity.isReady && (currentState.movementBlockingPoints -- entity.hitbox).intersect(entity.update[Movement](_.move(direction)).hitbox).isEmpty =>
            currentState
              .updateEntity(
                entityId,
                _.update[Movement](_.move(direction))
                  .resetInitiative()
              )
          case _ =>
            currentState
        }
      case (currentState, _) =>
        currentState
    }
    (updatedGamestate, Nil)
  }
}
